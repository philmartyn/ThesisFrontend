# NII Predictor App - M.Sc Thesis, Client/Server bits

This is essentially just a very simple Clojure web app and Jetty server to facilitate NII files to be loaded to a Python ML backend 
so that a prediction score of the presence of Bi-Polar can be returned to the user.

The NII files are put on a RabbitMQ RPC queue which sends off the request to the Python ML backend and waits for a response. The results are then printed as
a PDF on the web page. 

This was a very small part of my Master's thesis, the most amount of work was getting my head around the ML parts; I had no experience in ML prior to this.

I think this setup works well enough but could probably be improved upon. It was put on AWS but not in a very cloud friendly way. You could use AWS' queueing and Lambda functions instead of 
this approach, it might work a bit better.

The code is a bit rough in patches as it was fairly hectic getting the project completed, and I haven't gone back to it since. :)