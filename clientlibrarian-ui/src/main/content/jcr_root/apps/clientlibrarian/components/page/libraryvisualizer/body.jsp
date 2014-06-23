<%@include file="/libs/foundation/global.jsp" %>

<body>

    <h1>Library Visualizer</h1>

    <form id="formGraphProperties">
        <label for="pagePath">Page Path:</label>
        <input id="pagePath" name="path" type="text" value="/content/home" />
        <button>Graph it!</button>
    </form>
    <div id="visualizationControls">
        <div>Zoom Ratio: <span id="zoomRatio">1</span></div>
        <button id="zoomIn">+</button>
        <button id="zoomOut">-</button>
    </div>
    <div id="visualizationContainer"></div>

</body>