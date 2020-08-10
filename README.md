# jerryWeb
How the web works inside the client and server-side. it focuses on browser and web server.
This is composited with two works.
one is a toy web browser engine by java. another is a simple web application server.
</br>
![web_render](https://user-images.githubusercontent.com/13846660/28877364-1db9bc8e-77d7-11e7-8370-f15f47072f0c.PNG)
image from [limpet.net/mbrubeck](limpet.net/mbrubeck)</br>
</br>
The Toy web browser engine is influenced by limpet.net/mbrubeck's rust works.
A simple web application server is referenced from other open-source code.

usually, we don't know what is servlet container, and what is this doing?
even we can develop anything you want without knowing inside the browser and web application server.
especially when you are in a web-based system developer too.

But sometimes we want to know about the inside.
Not just admiring someone made.
and try-something even small is better than nothing.

* Result of interpreting

![preview](https://github.com/kimtth/jerryWeb/blob/master/org.web.labs.inside.jerry/src/jerry/test/rainbow.png?raw=true)

# Simple Container
A very simple way to implement.

as one sentence, the servlet container is a combination of the HTTP server and the class loader.
the servlet is one of the java programs that work on the web application server. 
sometimes web application server is called servlet container. 
and a servlet can make dynamic web pages programmatically. 

usually, apache tomcat has equipped many functions to realize for enterprise level.
not only class loader and having a Thread Pool and controlling the lifecycle of servlet and support web service and etc.

on the below architecture map, you will see some core concepts about Tomcat.
especially the main parts are Container and Connector and PipeLine task. 
the Valve is part of the Pipeline Task. it can have one and more before request or response to Container.
simply way, Valve looks like a wrapper class to each request and response. 
the Container is the most important part. it is controlling the servlet and socket's lifecycle. 
the Connector will be accepted in and outStream on a socket.

![container](https://user-images.githubusercontent.com/13846660/29071897-7e954c82-7c80-11e7-9487-0385ec5ccd2e.PNG)

◎ Container interface and there are four types of containers: Engine, Host, Context, and Wrapper.
　1) Engine. Represents the entire Catalina servlet engine. ex) Catalina</br>
　2) Host. Represents a virtual host with a number of contexts. ex) localhost</br>
　3) Context. Represents a web application. A context contains one or more wrappers. ex) docs, examples, host-managers, manager, ROOT(Application or webapps)
　4) Wrapper. Represents an individual servlet.</br>

each container has Realm, Valve, Logger. Ream is for Authentication, Logger is for logging.　
A pipeline contains tasks that the container will invoke. A valve represents a specific task. 
The valve is supplemented Request and Response.

These sequences of processing will be work on the HTTP protocol.
so you can send a request by web-browser or your client tool.

If you input and requesting a simple container by below URL.
http://localhost:8080/servlet/ToyServlet
You will see the result on the browser as below.

![toyserve](https://user-images.githubusercontent.com/13846660/29072151-8aa1bee2-7c81-11e7-82c0-b7c9043e1f67.png)

I used 8080 port for avoiding collide with a real HTTP server.

Typically Http is base on two-way transfer called request and response.
both sides are composite with header and body side.
each line of data was tokenized by CRLF. 
and sometimes not only a one-way request and response, HTTP 1.1 can send a data chunked way. 
Chunked data is composite with a sequence of "DataSize=Data".

I referenced Http Server in this link and using some part from the web.
[HTTP Server](http://qiita.com/opengl-8080/items/ca152658a0e52c786029)
[Understanding of Servlet Container](http://www.hanbit.co.kr/lib/examFileDown.php?hed_idx=1000)



