(ns thesis-frontend.retries
  "Stripped down version of https://github.com/jimpil/ajenda/blob/master/src/ajenda/retrying.clj.")

;; HELPERS
;; =======
(defn retryable-error
  [cause]
  (ex-info "Retryable error" {:retry true} cause))

(defn retryable-error?
  [x]
  (some-> x ex-data :retry))

(defonce do-nothing (constantly nil))

(defn max-retries-limiter
  "Returns `(partial >= max-retries)`."
  [max-retries]
  (assert (and (integer? max-retries)
               (not (neg? max-retries)))
          "Positive integer is required for <max-retries>.")
  ;;retries start from 1 (the very first attempt doesn't count as a retry!)
  (partial >= max-retries))

(defn thread-interrupted?
  "Wrapper around `Thread.isInterrupted`."
  ([_] ;; convenience overload (see `with-max-retries-timeout`)
   (thread-interrupted?))
  ([]
   (.isInterrupted (Thread/currentThread))))

(defn- catch-all*
  "Produces a list of `catch` clauses for all exception classes <exs>
   with the same <catch-tail>."
  [[_catch-all exs & catch-tail]]
  (map #(list* 'catch % catch-tail) exs))

(def ^:private supported-catches
  {"catch" 'catch
   "catch-all" 'catch-all})

(defmacro try-catch-all
  "Same as `clojure.core/try`, but also recognises `catch-all` clause(s).
   These must be in the same form as regular `catch`, but instead of a
   single exception class, you are expected to provide a vector of them."
  [& bodies]
  (let [[body catches] (reduce
                         (fn [res [fsymbol & args :as exp]]
                           (let [fname (name fsymbol)]
                             (if (contains? supported-catches fname)
                               (update res 1 conj (list* (get supported-catches fname) args))
                               (update res 0 conj exp))))
                         [[][]]
                         bodies)
        catch-all? #(and (seq? %)
                         (= (first %) 'catch-all))
        catch-all-guard (fn [form]
                          (if (catch-all? form)
                            (catch-all* form)
                            [form]))]
    `(try ~@body
          ~@(mapcat catch-all-guard catches))))


;; DEFAULTS
;; ========
(defn default-log-fn
  "Prints a rudimentary message to stdout about the current retrying attempt,
   and the (potential) upcoming delay (for which you need to use the 2-arg
   overload passing your `:delay-calc` fn as the first arg)."
  ([attempt]
   (default-log-fn (constantly 0) attempt))
  ([delay-calc attempt]
   (let [dlay-ms (delay-calc attempt)]
     (println
       (format "Attempt #%s failed! Retrying %s..."
               attempt
               (if (zero? dlay-ms)
                 "immediately"
                 (str "in " dlay-ms " ms")))))))

(defn default-delay-fn!
  "Blocks the thread via `(Thread/sleep ms)`."
  [ms]
  ;; need to be careful here as delaying strategies could be decreasing,
  ;; and in some cases they might start return negative values (e.g. additive delay)
  (when (some-> ms pos?)
    ;; NEVER attempt to call `.sleep()` on an interrupted thread!
    (when-not (thread-interrupted?)
      (Thread/sleep ms))))


;;GENERIC - CONDITION FOCUSED (bottom level utility)
;;=================================================

(defn with-retries*
  "Retries <f> (a fn of no args) until either <done?> (a fn of 1 arg: the result of `(f)`)
   returns a truthy value, in which case  the result of `(f)` is returned,
   or <retry?> (a fn of 1 arg - the current retry number) returns nil/false,
   in which case nil is returned.

   <opts> can be a map supporting the following options:

  :retry-fn!    A (presumably side-effecting) function of 1 argument (the current retrying attempt).
                Runs after the first attempt, and before each retry apart from the very last one
                (excluding timeout variants where it is not known when the last one will be).
                As such, this fn will never see `0` because it always refers to the upcoming retry.
                Logging can be implemented on top of this. See `default-log-fn` for an example.

  :delay-fn!    A function of 1 argument (the number of milliseconds) which blocks the current thread.
                Will run after the first attempt, and before each retry apart from the very last one.
                The default is `default-delay-fn!`, and it should suffice for the vast majority of use-cases.

  :delay-calc   A function of 1 argument (the current retrying attempt), returning the amount of milliseconds.
                This is expected to be a pure function, which can called more than once (e.g. by `:retry-fn!`).
                Batteries (ready-made calculators) are provided. See `fixed-delay`, `additive-delay`,
                `multiplicative-delay`, `exponential-delay`, `cyclic-delay` & `oscillating-delay` for examples.
                
  :error-message An exception message to display should the retries fail.
                "
  ([retry? done? f]
   (with-retries* retry? done? f nil))
  ([retry? done? f opts]
   (let [{:keys [retry-fn! delay-fn! delay-calc error-message]
          :or {delay-fn! default-delay-fn! 
               error-message "Not done after retries"}} opts
         ;; make a minimal fn given the <opts>
         ;; nothing happens for `retry-fn!` when it has not been provided (as one might expect)
         ;; nothing happens for `delay-fn!` when `delay-calc` has not been provided (delay-fn! depends on `delay-calc`)
         ;; nothing happens if neither `retry-fn!` or `delay-calc` have been provided => `(constantly nil)`
         retry!+delay! (if (fn? retry-fn!)
                         (if (fn? delay-calc)
                           (fn [i]
                             ;; do both
                             (retry-fn! i)
                             (delay-fn! (delay-calc i)))
                           (fn [i]
                             ;; only retry
                             (retry-fn! i)))
                         (if (fn? delay-calc)
                           (fn [i]
                             ;;only delay
                             (delay-fn! (delay-calc i)))
                           do-nothing))]
     (loop [result (f)
            ;; start from 0 - the very first attempt is not a retry!
            i 0]

       (if (done? result)
         result
         (let [next-i (unchecked-inc i)]
           (if (retry? next-i)
             (do
               ;; clever optimisation which looks one step ahead and skips the final retries/delays
               ;; this enables using 0 as the number of max-retries and no retries/delays will happen
               (retry!+delay! next-i)
               ;; What's the most sensible thing to do after Long/MAX_VALUE retries?
               ;; keep retrying with a bad counter, give up (i.e. throw), or keep retrying with
               ;; a good counter? I'd like to say that keep retrying is the right thing to do,
               ;; but at the same time I feel that auto-promoting (`inc'`)  would be an overkill here.
               ;; If someone retries something for that many times, chances are he doesn't care
               ;; about the number of attempts (e.g. timeout). So it seems `unchecked-inc` is the
               ;; most reasonable option here.
               (recur (f) next-i))

             ;; Finished trying
             (if (retryable-error? result)
               (throw (.getCause result))
               (throw (ex-info error-message
                               {:retries     i
                                :last-result result}))))))))))


;; EXCEPTION FOCUSED
;;==================
;; (see `https://github.com/jimpil/ajenda/blob/master/src/ajenda/retrying.clj` for `with-error-retries`)


(defmacro with-max-error-retries
  "The combination of `with-max-retries` & `with-error-retries`.
   Retries <body> until either <max-retries> is reached, or
   it doesn't throw one of <exceptions> (a vector of exception classes).
   <opts> as per `with-retries*`, including the following:
   :halt-on    A fn of 1 argument (the exception object thrown), responsible for deciding
               whether retrying should occur (or not). If it returns truthy value retrying stops.
               Provides finer control by giving a chance to recover from a particular exception."
  [opts max-retries exceptions & body]
  (assert (every? (partial instance? Class) (mapv resolve exceptions))
          "Exception classes are required for <exceptions>.")
  (cond-> `(with-retries*
             (max-retries-limiter ~max-retries)
             ;; success predicate - done if not retrying
             (complement retryable-error?)
             (fn []
               (try-catch-all
                 (do ~@body)
                 (catch-all ~exceptions e#
                            (if-let [pred# (:halt-on ~opts)]

                              (if (pred# e#)

                                ;; If :halt-on returns truthy we don't want to retry so we rethrow
                                (throw e#)

                                ;; Otherwise we indicate we want to retry
                                (retryable-error e#))

                              ;; Always retry if :halt-on not specified
                              (retryable-error e#))))))

          opts (concat `[~opts])))




(defn multiplicative-delay
  "Returns a function that will calculate the amount of delaying
   given the current retrying attempt, with multiplication semantics.
   At each retry you will get `(* ms (Math/pow multiplier retry))` delaying."
  [ms multiplier]
  (assert (pos? ms)
          "Negative or zero <ms> is NOT allowed!")
  (assert (and multiplier (> multiplier 1))
          "<multiplier> must be greater then 1 (see `fixed-delay` for that effect)...")

  (fn [i]
    ;; each delay blocks the next retry, so <i> will never be 0.
    ;; however in order to calculate it correctly we must take
    ;; into account the very first attempt too (hence need to decrement <i>)
    (long (* ms (Math/pow multiplier (unchecked-dec i))))))
