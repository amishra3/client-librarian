
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

// begin drawing graph
$(document).ready(function () {

    var container = d3.select('#visualizationContainer');

    $('#formGraphProperties').submit(function (e) {

        // clear out current container
        container.html('');

        // svg settings
        var width = 1500,
            height = 800,
            nodeRadius = 6,
            nodeCir = nodeRadius * 2,
            nodeTextPaddingLeft = 2,
            linkArrowHeight = 6;

        // intialize force graph
        var force = d3.layout.force()
            .size([width, height])
            .charge(-400)
            .linkDistance(40)
            .on('tick', tick);

        // set drag event
        var drag = force.drag()
            .on('dragstart', dragstart);

        // append new svg to container
        var svg = container.append('svg')
            .attr('width', width)
            .attr('height', height);

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
            svg.append('defs').selectAll("marker")
                .data(["arrowhead"])
                .enter().append("marker")
                .attr("id", String)
                .attr("viewBox", "0 -5 10 10")
                .attr("refX", nodeRadius + linkArrowHeight + 5)
                .attr("refY", 1)
                .attr("markerWidth", 6)
                .attr("markerHeight", linkArrowHeight)
                .attr("orient", "auto")
                .append("svg:path")
                .attr("d", "M0,-5L10,0L0,5");

            // set up links
            var link = svg.selectAll('.link')
                .data(graph.links)
                .enter().append('g')
                .attr('class', 'link');

            link.append('line')
                .attr('class', function (d) { return d.type; })
                .attr('marker-end', 'url(#arrowhead)');

            link.append('text')
                .attr('dy', '.35em')
                .text(function (d) { return d.type; });

            // set up nodes
            var node = svg.selectAll('.node')
                .data(graph.nodes)
                .enter().append('g')
                .attr('class', 'node');

            node.append('circle')
                .attr('class', function (d) { return d.type; })
                .attr('r', nodeRadius)
                .on('dblclick', dblclick)
                .call(drag);

            node.append('text')
                .attr('dy', '.35em')
                .text(function (d) { return d.id; });

        });

        // tick function that will render each frame of graph
        function tick() {

            // get all elements of graph
            var link = svg.selectAll('.link'),
                node = svg.selectAll('.node');

            // draw circles bounded by height and width of window
            node.select('circle')
                .attr('cx', function(d) { return d.x = Math.max(nodeCir, Math.min(width - nodeCir, d.x)); })
                .attr('cy', function(d) { return d.y = Math.max(nodeCir, Math.min(height - nodeCir, d.y)); });

            // draw text to the right of the circles
            node.select('text')
                .attr('x', function (d) { return d.x + nodeRadius + nodeTextPaddingLeft; })
                .attr('y', function (d) { return d.y; });

            // link circles with lines
            link.select('line')
                .attr('x1', function (d) { return d.source.x; })
                .attr('y1', function (d) { return d.source.y; })
                .attr('x2', function (d) { return d.target.x; })
                .attr('y2', function (d) { return d.target.y; });

            // add text to middle of link lines
            link.select('text')
                .attr('x', function (d) {
                    var dx = Math.abs(d.source.x - d.target.x) / 2;
                    var xn = (d.source.x > d.target.x) ? d.source.x - dx : d.source.x + dx;
                    return xn;
                })
                .attr('y', function (d) {
                    var dy = Math.abs(d.source.y - d.target.y) / 2;
                    var yn = (d.source.y > d.target.y) ? d.source.y - dy : d.source.y + dy;
                    return yn;
                });

        }

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
