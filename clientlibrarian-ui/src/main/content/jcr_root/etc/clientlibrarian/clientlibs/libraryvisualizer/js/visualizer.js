
/** TODOs:
 *      - fix weird dragging behavior when zoomed in
 *      - resize svg to fill entire background, overlay controls
 *      - change zooming from decimal to integer, makes more sense, won't hav t
 */

(function () {

    /**
     * Transform an edge from graph response into a link for d3.
     *
     * @param nodes     List of nodes in graph.
     * @param edge      Edge definition from response.
     * @returns {{type: *}}     Link to add to visual graph.
     */
    var edgeFromRespToLink = function (nodes, edge) {

        // begin link object
        var link = {
            type: edge.type
        };

        // loop through nodes to find which ones need to be connected
        for (var i = 0; i < nodes.length; i++) {

            // get current node
            var currNode = nodes[i];

            if(typeof(link.source) === "undefined" && edge.from === currNode.id) {

                // edge.from is the current node, set the link's source
                link.source = i;

            }

            if(typeof(link.target) === "undefined" && edge.to === currNode.id) {

                // edge.to is the current node, set the link's target
                link.target = i;

            }

        }

        // return built link
        return link;

    };

    /**
     * Turn response from graph servlet into visual graph for d3.
     *
     * @param resp      Response from servlet.
     * @returns {{}}    Visual graph to render in d3.
     */
    var respToGraph = function (resp) {

        var graph = {};

        // put nodes as they are into graph
        graph.nodes = resp.nodes;

        // transform edges to links for force graph
        graph.links = [];
        for (var i = 0; i < resp.edges.length; i++) {

            // build links
            var link = edgeFromRespToLink(graph.nodes, resp.edges[i]);
            graph.links.push(link);

        }

        return graph;

    };

    /**
     * Get (x,y) for a point on 1:1 plane relative to the viewport's origin on the 1:1 plane.
     *
     * @param viewportX     X position on 1:1 plane of viewport's origin.
     * @param viewportY     Y position on 1:1 plane of viewport's origin.
     * @param maxX          X maximum on 1:1 plane.
     * @param maxY          Y maximum on 1:1 plane.
     * @param zoomRatio     Ratio of zooming, used in factoring how scales operate.
     * @returns {{x: scale, y: scale}}  d3 scaling functions for x and y to be used to transform coordinates of objects
     *  to be displayed relative to viewport.
     */
    var calculateViewportScales = function (viewportX, viewportY, maxX, maxY, zoomRatio) {

        var inverseZoomRatio = 1/zoomRatio,
            rendVX = viewportX * inverseZoomRatio,
            rendVY = viewportY * inverseZoomRatio,
            rendMX = maxX * inverseZoomRatio,
            rendMY = maxY * inverseZoomRatio;

        var scaleX = d3.scale.linear()
                .domain([0, maxX])
                .range([-rendVX, rendMX - rendVX]),
            scaleY = d3.scale.linear()
                .domain([0, maxY])
                .range([-rendVY, rendMY - rendVY]);

        if (zoomRatio >= 1.0) {

            scaleX.clamp(true);
            scaleY.clamp(true);

        }

        return {
            x: scaleX,
            y: scaleY,
            graphMaxX: rendMX,
            graphMaxY: rendMY
        };

    };

    // svg settings
    var maxWidth = 800,             // actual pixel width of viewport and full graph
        maxHeight = 600,            // actual pixel height of viewport and full graph
        nodeRadius = 6,             // pixel radius of nodes
        nodeTextPaddingLeft = 2,    // pixel padding from node to text, placed left of the node
        linkArrowHeight = 6,        // height of arrow head
        fullyZoomedOutRatio = 1.0,  // zooming ratio at most zoomed out
        fullyZoomedInRatio = 0.1,   // zooming ratio at most zoomed in
        zoomTicks = 10;             // number of zooming options available between fully zoomed out and fully zoomed in

    // begin drawing graph
    $(document).ready(function () {

        var container = d3.select('#visualizationContainer'),
            $visualViewportCoordinates = $('#viewportOrigin'),
            $visualViewportZoom = $('#zoomRatio'),
            $buttonZoomIn = $('#zoomIn'),
            $buttonZoomOut = $('#zoomOut');

        $('#formGraphProperties').submit(function (e) {

            // create initial viewport variables
            var zoomOptions = d3.scale.linear().domain([fullyZoomedInRatio, fullyZoomedOutRatio]).ticks(zoomTicks),
                viewportOriginX = 0,
                viewportOriginY = 0,
                viewportPanMargin = 50,
                currZoomOption = zoomOptions.length - 1,
                viewportScale = calculateViewportScales(viewportOriginX, viewportOriginY,
                                                        maxWidth, maxHeight, zoomOptions[currZoomOption]);

            // clear out current container
            container.html('');

            // append new svg to container
            var svg = container.append('svg')
                .attr('width', maxWidth)
                .attr('height', maxHeight)
                .on('mousemove', function () {

                    // get mouse position for panning, use in calculating new viewport origin pos
                    var mousePos = d3.mouse(this),
                        mX = mousePos[0],
                        mY = mousePos[1],
                        nX = 0,
                        nY = 0;

                    // right and left movement
                    if (mX < viewportPanMargin) {

                        // nearing left margin of viewport, move viewport left
                        nX = viewportOriginX - viewportPanMargin;
                        viewportOriginX = nX < 0 ? 0 : nX;

                    } else if (mX > maxWidth - viewportPanMargin) {

                        // nearing right margin of viewport, move viewport right
                        nX = viewportOriginX + viewportPanMargin;
                        viewportOriginX = nX > maxWidth ? maxWidth - viewportOriginX : nX;

                    }

                    // up and down movement
                    if (mY < viewportPanMargin) {

                        // nearing top margin of viewport, move viewport up
                        nY = viewportOriginY - viewportPanMargin;
                        viewportOriginY = nY < 0 ? 0 : nY;

                    } if (mY > maxHeight - viewportPanMargin) {

                        nY = viewportOriginY + viewportPanMargin;
                        viewportOriginY = nY > maxHeight ? maxHeight - viewportOriginY : nY;

                    }

                    // refresh visual indicator of positioning
                    $visualViewportCoordinates.html('(' + viewportOriginX + ',' + viewportOriginY + ')');

                    // make new viewport scale functions
                    var currZoom = zoomOptions[currZoomOption];
                    viewportScale = calculateViewportScales(viewportOriginX, viewportOriginY,
                        maxWidth, maxHeight, currZoom);

                    // force a tick (rerender graph)
                    tick();

                });

            /**
             * Renders each animated frame of graph. Uses viewport scaling to render nodes based on their position
             *  relative to the viewport.
             */
            var tick = function () {

                // get all elements of graph
                var link = svg.selectAll('.link'),
                    node = svg.selectAll('.node');

                // draw circles bounded by height and width of window
                node.select('circle')
                    .attr('cx', function(d) {
                        d.scaledX = viewportScale.x(d.x);
                        return d.scaledX;
                    })
                    .attr('cy', function(d) {
                        d.scaledY = viewportScale.y(d.y);
                        return d.scaledY;
                    });

                // draw text to the right of the circles
                node.select('text')
                    .attr('x', function (d) { return d.scaledX + nodeRadius + nodeTextPaddingLeft; })
                    .attr('y', function (d) { return d.scaledY; });

                // link circles with lines
                link.select('line')
                    .attr('x1', function (d) { return d.source.scaledX; })
                    .attr('y1', function (d) { return d.source.scaledY; })
                    .attr('x2', function (d) { return d.target.scaledX; })
                    .attr('y2', function (d) { return d.target.scaledY; });

                // add text to middle of link lines
                link.select('text')
                    .attr('x', function (d) {
                        var dx = Math.abs(d.source.scaledX - d.target.scaledX) / 2;
                        return (d.source.scaledX > d.target.scaledX) ? d.source.scaledX - dx : d.source.scaledX + dx;
                    })
                    .attr('y', function (d) {
                        var dy = Math.abs(d.source.scaledY - d.target.scaledY) / 2;
                        return (d.source.scaledY > d.target.scaledY) ? d.source.scaledY - dy : d.source.scaledY + dy;
                    });

            };

            // initialize force graph
            var force = d3.layout.force()
                .size([maxWidth, maxHeight])
                .charge(-400)
                .linkDistance(40)
                .on('tick', tick);

            // set drag event
            var drag = force.drag()
                .on('dragstart', dragstart);

            // get the page path entered, hit servlet, get response, and render graph
            var $form = $(this);
            var pagePath = $form.find('#pagePath').val();
            d3.json('/bin/clientlibrarian/graph.json?page.path=' + pagePath, function(error, resp) {

                // transform response into graph for d3
                var graph = respToGraph(resp);

                // add items to force graph
                force
                    .nodes(graph.nodes)
                    .links(graph.links)
                    .start();

                // set up svg
                svg.append('defs').selectAll('marker')
                    .data(['arrowhead'])
                        .enter().append('marker')
                                .attr('id', String)
                                .attr('viewBox', '0 -5 10 10')
                                .attr('refX', nodeRadius + linkArrowHeight + 5)
                                .attr('refY', 1)
                                .attr('markerWidth', 6)
                                .attr('markerHeight', linkArrowHeight)
                                .attr('orient', 'auto')
                            .append('svg:path')
                                .attr('d', 'M0,-5L10,0L0,5');

                // set up links
                var link = svg.selectAll('.link')
                    .data(graph.links)
                        .enter().append('g')
                            .attr('class', 'link');

                // append lines
                link.append('line')
                    .attr('class', function (d) { return d.type; })
                    .attr('marker-end', 'url(#arrowhead)');

                // append line labels
                link.append('text')
                    .attr('dy', '.35em')
                    .text(function (d) { return d.type; });

                // set up nodes
                var node = svg.selectAll('.node')
                    .data(graph.nodes)
                    .enter().append('g')
                    .attr('class', 'node');

                // append circles
                node.append('circle')
                    .attr('class', function (d) { return d.type; })
                    .attr('r', nodeRadius)
                    .on('dblclick', dblclick)
                    .call(drag);

                // append node labels
                node.append('text')
                    .attr('dy', '.35em')
                    .text(function (d) { return d.id; });

                // set up controls
                var $zoomRatio = $('#zoomRatio');

                $buttonZoomIn.off('click').on('click', function () {

                    // recalculate zoom options
                    currZoomOption = currZoomOption - 1 < 0 ? 0 : currZoomOption - 1;
                    var currZoom = zoomOptions[currZoomOption];

                    // set zoom ratio thing
                    $visualViewportZoom.html(currZoom);

                    // make new viewport scale functions
                    viewportScale = calculateViewportScales(viewportOriginX, viewportOriginY,
                        maxWidth, maxHeight, currZoom);

                    // rescale force graph so it runs properly relative to viewport
                    force.size([viewportScale.graphMaxX, viewportScale.graphMaxY]).resume();

                    // force a tick (rerender graph)
                    tick();

                });

                $buttonZoomOut.off('click').on('click', function() {

                    // recalculate zoom options
                    currZoomOption = currZoomOption + 1 >= zoomOptions.length ?
                        zoomOptions.length - 1 : currZoomOption + 1;
                    var currZoom = zoomOptions[currZoomOption];

                    // set zoom ratio thing
                    $visualViewportZoom.html(currZoom);

                    // make new viewport scale functions
                    viewportScale = calculateViewportScales(viewportOriginX, viewportOriginY,
                        maxWidth, maxHeight, currZoom);

                    // rescale force graph so it runs properly relative to viewport
                    force.size([viewportScale.graphMaxX, viewportScale.graphMaxY]).resume();

                    // force a tick (rerender graph)
                    tick();

                });

            });

            // stop normal submit action
            e.preventDefault();

        });

        function dblclick(d) {
            d3.select(this).classed('fixed', d.fixed = false);
        }

        function dragstart(d) {
            d3.select(this).classed('fixed', d.fixed = true);
        }

    });

})();
