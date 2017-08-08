# jerryWeb
How web works inside client and server side. focus on browser and web server.</br>
This is composited with two works.</br>
One is Toy Web Browser Engine by Java. Another is Simple web application server.</br>
</br>
![web_render](https://user-images.githubusercontent.com/13846660/28877364-1db9bc8e-77d7-11e7-8370-f15f47072f0c.PNG)
image from [limpet.net/mbrubeck](limpet.net/mbrubeck)</br>
</br>
Toy Web Browser Engine is influenced from [limpet.net/mbrubeck](limpet.net/mbrubeck)'s rust works. </br>
Simple web application server is referenced from other open source code.</br>
</br>
Usually we don't know what is servlet container and what is this doing?</br>
Even we can devleop anything you want without knowing inside browser and web application server.</br>
Especillay when you are in a web based system developer too.</br>
</br>
But sometime we want to know about inside. </br>
not just admiring someone made.</br>
And try-something even small is better than nothing.</br>
</br>
# Simple Container
Very simple way to implementation.</br>
</br>
By one sentence, the servlet container is combinaton of HTTP Server and class loader.</br>
Servlet is one of Java Program that work on the Web Application Server. </br>
Web Application Server is sometime called servlet container. </br>
And servlet can be made Dynamic Web Page programmatically. </br>
</br>
Usually apache tomcat has equipped many function to realize for enterprise level.</br>
Not only class loader and have a Thread Pool and controlling lifecycle of servlet and etc.</br>
</br>
On the below architecture map, you will see the some of strange concept about Tomcat.</br>
especillay main parts are Container and Connector and PipeLine task. </br>
the Valve is part of Pipeline Task. it can be have one and more before request or response to Container.</br>
simply way, Valve looks like a wrapper class to each request and response. </br>
the Container is main of main. it is controlling Servlet and Socket's Lifecycle. </br>
the Connector will be accept In and outStream on a Socket.</br>
</br>
![container](https://user-images.githubusercontent.com/13846660/29071897-7e954c82-7c80-11e7-9487-0385ec5ccd2e.PNG)</br>
◎ Container interface and there are four types of containers: Engine, Host, Context, and Wrapper.</br>
　1) Engine. Represents the entire Catalina servlet engine. ex) Catalina</br>
　2) Host. Represents a virtual host with a number of contexts. ex) localhost</br>
　3) Context. Represents a web application. A context contains one or more wrappers. ex) docs, examples, host-managers, manager, ROOT(Application or webapps)</br>
　4) Wrapper. Represents an individual servlet.</br>
</br>
Each container has Realm, Valve, Logger. Ream is for Authentication, Logger is for logging.　</br>
A pipeline contains tasks that the container will invoke. A valve represents a specific task. </br>
Valve is supplemented Request and Response.</br>
</br>
These sequence of processing will be work on HTTP protocol.</br>
so you can send a request by web-browser or your client tool.</br>
</br>
if you input and requseting server by below url.</br>
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

