package ch.epfl.javelo.data;

import ch.epfl.javelo.Functions;
import ch.epfl.javelo.projection.PointCh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;


/**
 * 4.3.2
 * Graph
 * <p>
 * Classe offrant une méthode statique permettant de charger le graphe depuis
 * un répertoire.
 *
 * @author Jean Nordmann (344692)
 * @author Maxime Ducourau (329544)
 */

public final class Graph {

    /**
     * Attribut représentant les nœuds du graphe.
     */
    private final GraphNodes nodes;

    /**
     * Attribut représentant les secteurs du graphe.
     */
    private final GraphSectors sectors;

    /**
     * Attribut représentant les arêtes du graphe.
     */
    private final GraphEdges edges;

    /**
     * Attribut représentant la liste des ensembles d'attributs donnés.
     */
    private final List<AttributeSet> attributeSets;

    /**
     * Constructeur public initialisant les attributs de la classe à leurs valeurs passées en
     * paramètres.
     * @param nodes Nœuds donnés.
     * @param sectors Secteurs donnés.
     * @param edges Arêtes données.
     * @param attributeSets Ensemble d'attributs donnés.
     */
    public Graph(GraphNodes nodes, GraphSectors sectors, GraphEdges edges,
                 List<AttributeSet> attributeSets) {
        this.nodes = nodes;
        this.sectors = sectors;
        this.edges = edges;
        this.attributeSets = List.copyOf(attributeSets);

    }

    /**
     * Méthode privée pour éviter la répétition de code.
     * Elle permet de lire un fichier bin et de le retourner en ByteBuffer.
     * ATTENTION à ensuite "recast" en Buffer du type voulu (int/short...)
     *
     * @param basePath Chemin d'accès donné.
     * @return Retourne le fichier bin lu en ByteBuffer.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */

    private static ByteBuffer mappedBuffer(Path basePath) throws IOException {
        try (FileChannel channel = FileChannel.open(basePath)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

    /**
     * Retourne le graphe Javelo obtenu à partir des fichiers se trouvant dans le répertoire, ou
     * bien lance une exception en cas d'erreur d'entrée ou de sortie.
     * @param basePath Chemin d'accès donné.
     * @return Le graphe Javelo obtenu à partir des fichiers se trouvant dans le répertoire.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */

    public static Graph loadFrom(Path basePath) throws IOException {
        //Chargement des différents attributs du graph
        //Nodes
        Path nodesPath = basePath.resolve("nodes.bin");
        IntBuffer nodeBuffer = mappedBuffer(nodesPath).asIntBuffer();
        GraphNodes nodes = new GraphNodes(nodeBuffer);

        //Sectors
        Path sectorsPath = basePath.resolve("sectors.bin");
        ByteBuffer sectorBuffer = mappedBuffer(sectorsPath);
        GraphSectors sectors = new GraphSectors(sectorBuffer);

        //Edges
        Path edgesPath = basePath.resolve("edges.bin");
        ByteBuffer edgeBuffer = mappedBuffer(edgesPath);
        Path profileIdsPath = basePath.resolve("profile_ids.bin");
        IntBuffer profilBuffer = mappedBuffer(profileIdsPath).asIntBuffer();
        Path elevationPath = basePath.resolve("elevations.bin");
        ShortBuffer elevationBuffer = mappedBuffer(elevationPath).asShortBuffer();
        GraphEdges edges = new GraphEdges(edgeBuffer, profilBuffer, elevationBuffer);

        //Attributes
        Path attributesPath = basePath.resolve("attributes.bin");
        LongBuffer attributeBuffer = mappedBuffer(attributesPath).asLongBuffer();
        List<AttributeSet> attributeSets = new ArrayList<>();
        for (int i = 0; i < attributeBuffer.capacity(); i++) {
            attributeSets.add(new AttributeSet(attributeBuffer.get(i)));
        }
        return new Graph(nodes, sectors, edges, attributeSets);
    }

    /**
     * Retourne le nombre total de nœuds dans le graphe.
     * @return Le nombre total de nœuds dans le graphe.
     */

    public int nodeCount() {
        return nodes.count();
    }

    /**
     * Retourne la position géographique du nœud donné.
     * @param nodeId Identité du nœud donné.
     * @return La position géographique du nœud donné.
     */

    public PointCh nodePoint(int nodeId) {
        return new PointCh(nodes.nodeE(nodeId), nodes.nodeN(nodeId));
    }

    /**
     * Retourne le nombre d'arêtes sortant du nœud d'identité donné.
     * @param nodeId Identité du nœud donné.
     * @return Retourne le nombre d'arêtes sortant du nœud d'identité donné.
     */

    public int nodeOutDegree(int nodeId) {
        return nodes.outDegree(nodeId);
    }

    /**
     * Retourne l'identité de edgeIndex-ième arête sortant du nœud d'identité
     * donné.
     * @param nodeId    Identité du nœud donné.
     * @param edgeIndex Index de l'arête vis-à-vis de la première arête du nœud.
     * @return Retourne l'identité de la edgeIndex-ième arête sortant du nœud
     * d'identité donné.
     */

    public int nodeOutEdgeId(int nodeId, int edgeIndex) {
        return nodes.edgeId(nodeId, edgeIndex);
    }

    /**
     * Retourne l'identité du nœud se trouvant le plus proche du point donné, à
     * la distance maximale donnée (en mètres), ou -1 si aucun nœud ne correspond à
     * ces critères.
     * @param point          Point donné.
     * @param searchDistance Distance maximale de recherche donnée.
     * @return Retourne l'identité du nœud se trouvant le plus proche du point
     * donné, à la distance maximale donnée (en mètres), ou -1 si aucun nœud ne
     * correspond à ces critères.
     */

    public int nodeClosestTo(PointCh point, double searchDistance) {
        List<GraphSectors.Sector> sectorList = sectors.sectorsInArea(point, searchDistance);
        double min = searchDistance * searchDistance;
        int nodeId = -1;
        //Itère sur tous les secteurs du Graph.
        for (GraphSectors.Sector sector : sectorList) {
            int startNode = sector.startNodeId();
            int endNode = sector.endNodeId();
            for (int j = startNode; j < endNode; j++) {
                //Compare les distances au carré au lieu des distances pour gagner de l'efficacité.
                double squaredDistance = point.squaredDistanceTo(nodePoint(j));
                if (squaredDistance <= min) {
                    min = squaredDistance;
                    nodeId = j;
                }
            }
        }
        return nodeId;
    }

    /**
     * Retourne l'identité du nœud destination de l'arête d'identité donnée.
     * @param edgeId Identité de l'arête donnée.
     * @return Retourne l'identité du nœud destination de l'arête d'identité donnée.
     */

    public int edgeTargetNodeId(int edgeId) {
        return edges.targetNodeId(edgeId);
    }

    /**
     * Retourne vrai si et seulement si l'arête d'identité donnée va dans le sens
     * contraire de la voie OSM dont elle provient.
     * @param edgeId Identité de l'arête donnée.
     * @return Retourne vrai si et seulement si l'arête d'identité donnée va dans le
     * sens contraire de la voie OSM dont elle provient.
     */

    public boolean edgeIsInverted(int edgeId) {
        return edges.isInverted(edgeId);
    }

    /**
     * Retourne l'ensemble des attributs OSM attachés à l'arête d'identité donnée.
     * @param edgeId Identité de l'arête donnée.
     * @return Retourne l'ensemble des attributs OSM attachés à l'arête d'identité donnée.
     */

    public AttributeSet edgeAttributes(int edgeId) {
        return attributeSets.get(edges.attributesIndex(edgeId));
    }

    /**
     * Retourne la longueur de l'arête d'identité donnée.
     * @param edgeId Identité de l'arête donnée.
     * @return La longueur de l'arête d'identité donnée.
     */

    public double edgeLength(int edgeId) {
        return edges.length(edgeId);
    }

    /**
     * Retourne le dénivelé positif total de l'arête donnée.
     * @param edgeId Identité de l'arête donnée.
     * @return Retourne le dénivelé positif total de l'arête donnée.
     */

    public double edgeElevationGain(int edgeId) {
        return edges.elevationGain(edgeId);
    }

    /**
     * Retourne le profil en long de l'arête d'identité donnée, sous la forme d'une
     * fonction ; si l'arête ne possède pas de profil, alors cette fonction doit
     * retourner Double.NaN pour n'importe quel argument.
     * @param edgeId Identité de l'arête donnée.
     * @return Retourne le profil en long de l'arête d'identité donnée, sous la forme
     * d'une fonction ; si l'arête ne possède pas de profil, alors cette fonction doit
     * retourner Double.NaN pour n'importe quel argument.
     */

    public DoubleUnaryOperator edgeProfile(int edgeId) {
        return edges.hasProfile(edgeId) ? Functions.sampled(edges.profileSamples(edgeId),
                edgeLength(edgeId)) : Functions.constant(Double.NaN);
    }
}
