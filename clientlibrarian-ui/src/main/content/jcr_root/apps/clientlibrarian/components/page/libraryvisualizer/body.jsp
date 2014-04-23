<%@include file="/libs/foundation/global.jsp" %>

<body>

    <h1>Library Visualizer</h1>

    <form id="formGraphProperties">
        <label for="pagePath">Page Path:</label>
        <input id="pagePath" name="path" type="text" value="/content/home" />
        <button>Graph it!</button>
    </form>
    <div id="visualizationContainer"></div>

</body>