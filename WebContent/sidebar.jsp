<%-- ===================================================================
     sidebar.jsp — shared left-hand navigation, included by every panel via
     <%@ include file="sidebar.jsp" %>. Because that is a STATIC include, the
     variable `activePage` declared by the including page is visible here and
     is used to highlight the current nav item. Each page sets, e.g.:
         <% String activePage = "dataset"; %>
=================================================================== --%>
<%
    String __active = (activePage == null) ? "" : activePage;
%>
<aside class="sidebar">
    <div class="brand">
        &#127780; Weather Analytics
        <small>Mini Project 2</small>
    </div>
    <nav>
        <a href="browse" class="<%= "dataset".equals(__active) ? "active" : "" %>">Dataset Management</a>
        <%-- Analysis + Export both load analytics.jsp; while ON that page, JS
             intercepts these clicks to switch tabs WITHOUT a reload (so the
             live stream survives). On other pages they navigate normally. --%>
        <a id="nav-analysis" href="analytics.jsp?tab=analysis" class="<%= "analysis".equals(__active) ? "active" : "" %>">Analysis</a>
        <a id="nav-export"   href="analytics.jsp?tab=export"   class="<%= "export".equals(__active)   ? "active" : "" %>">Export</a>
    </nav>
    <div class="sidebar-foot">BITS 3515 &middot; TCP/IP Programming</div>
</aside>
