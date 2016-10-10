<%-- 
    Document   : index
    Created on : Feb 23, 2016, 4:15:04 PM
    Author     : vidhi
--%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.adaptiveweb.webapp.lucenemaven"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" href="public/style.css">
        <title>Lucene Recommendation</title>
    </head>
    <body>
        <% lucenemaven lm = new lucenemaven();
        lm.main();
        %>    
        <div>The following section displays top 10 recommended items crawled from wikibooks and indexed via Lucene with keywords for each of the 10 posts. </div>
        <h2>Recommended Posts</h2>
        <ul>
            <% for (int i = 0; i < lm.posts.size(); i++) {
                
                ArrayList<String> temp = lm.posts.get(i);%>
            <li>
                <input type="checkbox" checked>
                <i></i>
                <h2>Post- <%=i+1%> </h2>
                <h5 style="color:red;"><%lm.post_title.get(i);%></h5>
                <%for (int j = 0; j < temp.size(); j++) {%>
                <b>Item-<%=j+1%></b><p><%=temp.get(j)%></p>
                <% } %>
            </li>
            <% } %>
        </ul>
        <div>
            Indexing was done using Lucene indexwriter:
            Create a method to get a lucene document from a text file.

            Create various types of fields which are key value pairs containing keys as names and values as contents to be indexed.

            Set field to be analyzed or not. In our case, only contents is to be analyzed as it can contain data such as a, am, are, an etc. which are not required in search operations.

            Add the newly created fields to the document object and return it to the caller method.
        </div>
        <div>
            Extra effort - Extra characters, symbols were rooted out from the posts text from csv using Lucene libraries. Stemming was done using Lucene PorterStemFilter.
            </div>
    </body>
</html>
