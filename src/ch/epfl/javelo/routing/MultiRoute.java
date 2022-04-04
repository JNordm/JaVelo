package ch.epfl.javelo.routing;

import ch.epfl.javelo.Math2;
import ch.epfl.javelo.Preconditions;
import ch.epfl.javelo.projection.PointCh;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 6.3.1
 * MultiRoute
 *
 * Classe publique et immuable, représentant un itinéraire multiple,
 * c'est-à-dire composé d'une séquence d'itinéraires contigus nommés
 * segments. Elle implémente l'interface Route et possède un unique
 * constructeur public.
 *
 * @author Jean Nordmann (344692)
 * @author Maxime Ducourau (329544)
 */

public final class MultiRoute implements Route {

    /**
     * Attribut représentant une liste de Route. Les éléments peuvent
     * être d'autres MultiRoute ou des SingleRoute, c'est pourquoi nous
     * ne pouvons pas être plus précis que la classe Route.
     */
//todo remettre en privé
    public final List<Route> segments;

    /**
     * @param segments Segments donnés.
     * Constructeur qui instancie un itinéraire multiple composé des
     * segments donnés, ou lève une IllegalArgumentException si la liste
     * des segments est vide.
     */

    public MultiRoute(List<Route> segments) {
        Preconditions.checkArgument(!segments.isEmpty());
        this.segments = List.copyOf(segments);
    }

    /**
     * @param position Position donnée.
     * @return Retourne l'index du segment de l'itinéraire contenant la
     * position donnée.
     */

    @Override
    public int indexOfSegmentAt(double position) {
        double actualPosition = 0, previousPosition = 0;
        int index = 0;
        position = Math2.clamp(0, position, this.length());
        for (Route segment : segments) {
            actualPosition += segment.length();
            if (position <= actualPosition) {
                index += segment.indexOfSegmentAt(position - previousPosition);
                return index;
            }
            index += segment.indexOfSegmentAt(actualPosition) + 1;
            previousPosition += segment.length();
        }
        return 0;
    }

    /**
     * @return La longueur de l'itinéraire en mètres.
     */

    @Override
    public double length() {
        double length = 0;
        for (Route segment : segments) {
            length += segment.length();
        }
        return length;
    }

    /**
     * @return La totalité des arêtes de l'itinéraire.
     */

    @Override
    public List<Edge> edges() {
        List<Edge> edgeList = new ArrayList<>();
        for (Route segment : segments) {
            edgeList.addAll(segment.edges());
        }
        return List.copyOf(edgeList);
    }

    /**
     * @return Retourne la totalité des points situés aux
     * extrémités des arêtes de l'itinéraire, sans doublons.
     */

    @Override
    public List<PointCh> points() {
        List<PointCh> pointChList = new ArrayList<>();

        for (Route segment : segments) {
            pointChList.addAll(segment.points());
            pointChList.remove(pointChList.size() - 1);
        }
        pointChList.add(segments.get(segments.size() - 1).pointAt(segments.get(segments.size() - 1).length()));
        return List.copyOf(pointChList);
    }

   /**
     * @param position Position donnée.
     * @return Retourne le point se trouvant à la position donnée le
     * long de l'itinéraire.
     */

    @Override
    public PointCh pointAt(double position) {
        double actualPosition = 0, previousPosition = 0;
        position = Math2.clamp(0, position, this.length());
        for (Route segment : segments) {
            actualPosition += segment.length();
            if (position <= actualPosition) return segment.pointAt(position - previousPosition);
            previousPosition += segment.length();
        }
        return null;
    }

    /**
     * @param position Position donnée.
     * @return Retourne l'altitude à la position donnée le long
     * de l'itinéraire, qui peut valoir NaN si l'arête contenant
     * cette position n'a pas de profil.
     */

    @Override
    public double elevationAt(double position) {
        double actualPosition = 0, previousPosition = 0;
        position = Math2.clamp(0, position, this.length());
        for (Route segment : segments) {
            actualPosition += segment.length();
            if (position <= actualPosition) return segment.elevationAt(position - previousPosition);
            previousPosition += segment.length();
        }
        return 0;
    }

    /**
     * @param position Position donnée.
     * @return Retourne l'identité du nœud appartenant à l'itinéraire et se
     * trouvant le plus proche de la position donnée.
     */

    @Override
    public int nodeClosestTo(double position) {
        double nextPosition = 0, previousPosition =0;
        double clampedPosition = Math2.clamp(0, position, this.length());
        for (Route segment : segments) {
            nextPosition += segment.length();
        if (clampedPosition - nextPosition <= 0) return segment.nodeClosestTo(clampedPosition - previousPosition);
            previousPosition += segment.length();
        }
        return 0;
    }

    /**
     * @param point Point de référence donné.
     * @return Retourne le point de l'itinéraire se trouvant le plus proche du
     * point de référence donné.
     */

    @Override
    public RoutePoint pointClosestTo(PointCh point) {
        RoutePoint routePointTemp, routePoint = RoutePoint.NONE;
        double actualPosition = 0;
        for (Route segment : segments) {
            routePointTemp = segment.pointClosestTo(point);
            routePoint = routePoint.min(routePointTemp.point(), actualPosition + routePointTemp.position(),
                    routePointTemp.distanceToReference());
            actualPosition += segment.length();
        }
        return routePoint;
    }

    //Utile pour comparer des objets de type MultiRoute dans les tests,
    //qui sans cette méthode, comparent les références des objets, et pas
    //leurs contenus.

    //checker l'imuablilitlé
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiRoute that = (MultiRoute) o;
        return Objects.equals(segments, that.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segments);
    }
}
