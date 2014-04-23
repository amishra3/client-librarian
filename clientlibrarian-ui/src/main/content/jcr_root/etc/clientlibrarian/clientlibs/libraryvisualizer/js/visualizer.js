
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
            height = 500;

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

        var link = svg.selectAll('.link'),
            node = svg.selectAll('.node');

        var $form = $(this);
        var pagePath = $form.find('#pagePath').val();
        d3.json('/bin/clientlibrarian/graph.json?page.path=' + pagePath, function(error, resp) {

            var graph = respToGraph(resp);

            force
                .nodes(graph.nodes)
                .links(graph.links)
                .start();

            link = link.data(graph.links)
                .enter().append('line')
                .attr('class', function (d) { return 'link ' + d.type; });

            node = node.data(graph.nodes)
                .enter().append('circle')
                .attr('class', function (d) { return 'node ' + d.type; })
                .attr('r', 12)
                .on('dblclick', dblclick)
                .call(drag);
        });

        function tick() {
            link.attr('x1', function(d) { return d.source.x; })
                .attr('y1', function(d) { return d.source.y; })
                .attr('x2', function(d) { return d.target.x; })
                .attr('y2', function(d) { return d.target.y; });

            node.attr('cx', function(d) { return d.x; })
                .attr('cy', function(d) { return d.y; });
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
