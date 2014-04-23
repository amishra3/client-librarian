
var edgeFromRespToLink = function (nodes, edge) {

    var link = {
        type: edge.type
    };

    for (var i = 0; i < nodes.length; i++) {

        var currNode = nodes[i];
        if(typeof(link.source) === "undefined" && edge.from === currNode.id) {

            link.source = i;

        }

        if(typeof(link.target) === "undefined" && edge.to === currNode.id) {

            link.target = i;

        }

    }

    return link;

};

var respToGraph = function (resp) {

    var graph = {};

    // put nodes as they are into graph
    graph.nodes = resp.nodes;

    // transform edges to links for force graph
    graph.links = [];
    for (var i = 0; i < resp.edges.length; i++) {

        var link = edgeFromRespToLink(graph.nodes, resp.edges[i]);
        graph.links.push(link);

    }

    return graph;

};


$(document).ready(function () {

    var container = d3.select('#visualizationContainer');

    $('#formGraphProperties').submit(function (e) {

        // clear out current container
        container.html('');

        var width = 960,
            height = 500,
            nodeRadius = 6,
            nodeTextPaddingLeft = 2;

        var force = d3.layout.force()
            .size([width, height])
            .charge(-400)
            .linkDistance(40)
            .on('tick', tick);

        var drag = force.drag()
            .on('dragstart', dragstart);

        // append new svg
        var svg = container.append('svg')
            .attr('width', width)
            .attr('height', height);

        var $form = $(this);
        var pagePath = $form.find('#pagePath').val();
        d3.json('/bin/clientlibrarian/graph.json?page.path=' + pagePath, function(error, resp) {

            var graph = respToGraph(resp);

            force
                .nodes(graph.nodes)
                .links(graph.links)
                .start();

            var link = svg.selectAll('.link').data(graph.links).enter();

            link.append('line')
                .attr('class', function (d) { return 'link ' + d.type; });

            link.append('text')
                .attr('dx', 12)
                .attr('dy', '.35em')
                .text(function (d) { return d.type; });

            var node = svg.selectAll('.node')
                .data(graph.nodes)
                .enter().append('g')
                .attr('class', 'node');

            node.append('circle')
                .attr('class', function (d) { return d.type; })
                .attr('r', nodeRadius)
                .on('dbclick', dblclick)
                .call(drag);

            node.append('text')
                .attr('dy', '.35em')
                .text(function (d) { return d.id; });

        });

        function tick() {
            var link = svg.selectAll('.link'),
                node = svg.selectAll('.node');

            link.attr('x1', function(d) { return d.source.x; })
                .attr('y1', function(d) { return d.source.y; })
                .attr('x2', function(d) { return d.target.x; })
                .attr('y2', function(d) { return d.target.y; });

            node.select('circle')
                .attr('cx', function(d) { return d.x; })
                .attr('cy', function(d) { return d.y; });

            node.select('text')
                .attr('x', function (d) { return d.x + nodeRadius + nodeTextPaddingLeft; })
                .attr('y', function (d) { return d.y; });

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
