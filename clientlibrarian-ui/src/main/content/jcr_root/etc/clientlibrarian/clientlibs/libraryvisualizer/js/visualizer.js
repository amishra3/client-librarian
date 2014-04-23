
var respToGraph = function(resp) {

// TODO : look at (http://bl.ocks.org/mbostock/3750558) for how nodes should be, need to take response and trans into that format

     return {
         nodes: [],
         links: []
     };

};


$(document).ready(function() {

    var width = 960,
        height = 500;

    var force = d3.layout.force()
        .size([width, height])
        .charge(-400)
        .linkDistance(40)
        .on('tick', tick);

    var drag = force.drag()
        .on('dragstart', dragstart);

    var svg = d3.select('body').append('svg')
        .attr('width', width)
        .attr('height', height);

    var link = svg.selectAll('.link'),
        node = svg.selectAll('.node');

// TODO : this all needs to be refactored
    var pagePath = '/content/home';
    d3.json('/bin/clientlibrarian/graph.json?page.path=' + pagePath, function(error, resp) {

        var graph = respToGraph(resp);

        force
            .nodes(graph.nodes)
            .links(graph.edges)
            .start();

        link = link.data(graph.links)
            .enter().append('line')
            .attr('class', 'link');

        node = node.data(graph.nodes)
            .enter().append('circle')
            .attr('class', 'node')
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

    function dblclick(d) {
        d3.select(this).classed('fixed', d.fixed = false);
    }

    function dragstart(d) {
        d3.select(this).classed('fixed', d.fixed = true);
    }

});
