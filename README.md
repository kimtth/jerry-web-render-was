# jerryWeb
How web works inside client and server side. focus on browser and web server.</br>
This is composited with two works.</br>
one is toy web browser engine by java. another is simple web application server.</br>
</br>
![web_render](https://user-images.githubusercontent.com/13846660/28877364-1db9bc8e-77d7-11e7-8370-f15f47072f0c.PNG)
image from [limpet.net/mbrubeck](limpet.net/mbrubeck)</br>
</br>
Toy web browser engine is influenced from [limpet.net/mbrubeck](limpet.net/mbrubeck)'s rust works. </br>
Simple web application server is referenced from other open source code.</br>
</br>
usually we don't know what is servlet container and what is this doing?</br>
even we can devleop anything you want without knowing inside browser and web application server.</br>
especillay when you are in a web based system developer too.</br>
</br>
But sometime we want to know about inside. </br>
Not just admiring someone made.</br>
and try-something even small is better than nothing.</br>
</br>
# Simple Container
very simple way to implementation.</br>
</br>
as one sentence, the servlet container is combinaton of HTTP server and class loader.</br>
servlet is one of java program that work on the web application server. </br>
sometime web application server is called servlet container. </br>
and servlet can make dynamic web page programmatically. </br>
</br>
usually apache tomcat has equipped many function to realize for enterprise level.</br>
not only class loader and having a Thread Pool and controlling lifecycle of servlet and support web service and etc.</br>
</br>
on the below architecture map, you will see the some of strange concept about Tomcat.</br>
especillay main parts are Container and Connector and PipeLine task. </br>
the Valve is part of Pipeline Task. it can be have one and more before request or response to Container.</br>
simply way, Valve looks like a wrapper class to each request and response. </br>
the Container is most important part. it is controlling servlet and socket's lifecycle. </br>
the Connector will be accept in and outStream on a socket.</br>
</br>
![container](https://user-images.githubusercontent.com/13846660/29071897-7e954c82-7c80-11e7-9487-0385ec5ccd2e.PNG)</br>
◎ Container interface and there are four types of containers: Engine, Host, Context, and Wrapper.</br>
　1) Engine. Represents the entire Catalina servlet engine. ex) Catalina</br>
　2) Host. Represents a virtual host with a number of contexts. ex) localhost</br>
　3) Context. Represents a web application. A context contains one or more wrappers. ex) docs, examples, host-managers, manager, ROOT(Application or webapps)</br>
　4) Wrapper. Represents an individual servlet.</br>
</br>
each container has Realm, Valve, Logger. Ream is for Authentication, Logger is for logging.　</br>
A pipeline contains tasks that the container will invoke. A valve represents a specific task. </br>
Valve is supplemented Request and Response.</br>
</br>
These sequence of processing will be work on HTTP protocol.</br>
so you can send a request by web-browser or your client tool.</br>
</br>
If you input and requseting simple container by below url.</br>
http://localhost:8080/servlet/ToyServlet</br>
You will see the result on the browser as a below.</br>
![toyserve](https://user-images.githubusercontent.com/13846660/29072151-8aa1bee2-7c81-11e7-82c0-b7c9043e1f67.png)</br>
I used 8080 port for avoiding collide with real http server.</br>
</br>
Typically Http is base on two-way transfer called request and response.</br>
both sides are composite with header and body side.</br>
each line of data was tokenized by CRLF. </br>
and sometime not only one way request and response, HTTP 1.1 can be send a data chunked way. </br>
Chunked data is composite with sequence of "DataSize=Data".</br>
</br>
I referenced Http Server in this link and using a some part from web.</br>
[HTTP Server](http://qiita.com/opengl-8080/items/ca152658a0e52c786029)</br>
[Understanding of Servlet Container](http://www.hanbit.co.kr/lib/examFileDown.php?hed_idx=1000)</br>
</br>

# Extra
The name jerryWeb is coming from most famous servlet container Tomcat.</br> 
I don't know exactly where is the name coming from ? but maybe tom & jerry animation. i think. </br>
So i named my repository as a jerryWeb. :)</br>
</br>

