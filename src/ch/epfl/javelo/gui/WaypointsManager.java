package ch.epfl.javelo.gui;

import ch.epfl.javelo.data.Graph;
import ch.epfl.javelo.projection.PointCh;
import ch.epfl.javelo.projection.PointWebMercator;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 8.3.3
 * WaypointManager
 * <p>
 * Classe gérant l'affichage et l'interaction avec les points de passage.
 *
 * @author Jean Nordmann (344692)
 * @author Maxime Ducourau (329544)
 */
public final class WaypointsManager {

    private final int SEARCH_DISTANCE = 500;

    private final Graph graph;
    private final ObjectProperty<MapViewParameters> mapViewParameters;
    private final ObservableList<Waypoint> waypointList;
    private final Consumer<String> stringConsumer;
    private final Pane pane;

    /**
     * Classe gérant l'affichage et l'interaction avec les points de passage.
     * @param graph graph du réseau routier.
     * @param mapViewParameters paramètres du fond de carte actuel.
     * @param waypointList liste observable de tous les points de passages.
     * @param stringConsumer consumer nous permettant de signaler les erreurs à afficher sur l'interface graphique.
     */
    public WaypointsManager(Graph graph, ObjectProperty<MapViewParameters> mapViewParameters,
                            ObservableList<Waypoint> waypointList, Consumer<String> stringConsumer) {
        this.graph = graph;
        this.mapViewParameters = mapViewParameters;
        this.waypointList = waypointList;
        this.stringConsumer = stringConsumer;
        //TODO essayer avec un seul et même groupe.
        pane = new Pane();
        for (int i = 0; i < waypointList.size(); i++) {
            Group group = new Group(getAndSetOutsideBorder(), getAndSetInsideBorder());
            group.getStyleClass().add("pin");
            pane.getChildren().add(group);
        }
        refreshGroups();
        pane.setPickOnBounds(false);
    }

    /**
     *
     * @return
     */
    public Pane pane() { return pane; }

    public void draw() {
        System.out.println(pane.getChildren().get(0).getStyleClass().toString());
        System.out.println(pane.getChildren().get(1).getStyleClass().toString());
        for (int i = 0; i < waypointList.size(); i++) {
            PointWebMercator pointWebMercator = PointWebMercator.ofPointCh(waypointList.get(i).pointCh());
            double xWayPoint = mapViewParameters.get().viewX(pointWebMercator);
            double yWayPoint = mapViewParameters.get().viewY(pointWebMercator);
            pane.getChildren().get(i).setLayoutX(xWayPoint);
            pane.getChildren().get(i).setLayoutY(yWayPoint);
        }
        pane.setPickOnBounds(false);
    }
    /**
     * Méthode permettant d'ajouter une point de passage sur la carte à l'aide de ses coordonnées relatives
     * sur la carte affichée à l'écran.
     * @param x coordonnée X partante depuis le coin haut gauche
     * @param y coordonnée Y partante depuis le coin haut gauche
     */
    public void addWaypoint(double x, double y) {
        PointWebMercator pointWebMercator = mapViewParameters.get().pointAt(x, y);
        PointCh pointCh = pointWebMercator.toPointCh();
        int idNodeClosestTo = graph.nodeClosestTo(pointCh, SEARCH_DISTANCE);
        if (idNodeClosestTo == -1) {
            // Pas de nœud dans la distance de recherche.
            //THROW ...
            stringConsumer.accept("Erreur pas de noeud dans la distance de recherche.");
            // stringConsumer.accept(() -> System.out.println("1 Erreur pas de nœud dans la distance de recherche.");

            System.out.println("une exception devrait être affichée sur l'interface graphique");
        } else {
            // Ajout du Waypoint à liste des Waypoint.
            waypointList.add(new Waypoint(graph.nodePoint(idNodeClosestTo), idNodeClosestTo));
            Group group = new Group(getAndSetOutsideBorder(), getAndSetInsideBorder());
            group.getStyleClass().add("pin");
            pane.getChildren().add(group);
            refreshGroups();
        }
    }

    public SVGPath getAndSetOutsideBorder() {
        SVGPath outsideBorder = new SVGPath();
        outsideBorder.setContent("M-8-20C-5-14-2-7 0 0 2-7 5-14 8-20 20-40-20-40-8-20");
        outsideBorder.getStyleClass().add("pin_outside");
        return outsideBorder;
    }

    public SVGPath getAndSetInsideBorder() {
        SVGPath insideBorder = new SVGPath();
        insideBorder.setContent("M0-23A1 1 0 000-29 1 1 0 000-23");
        insideBorder.getStyleClass().add("pin_inside");
        return insideBorder;
    }

    public void refreshGroups() {
        ObservableList<Node> nodes = pane.getChildren();
        nodes.get(0).getStyleClass().add("first");
        if(nodes.size() > 1) nodes.get(nodes.size() - 1).getStyleClass().add("last");
        for (int i = 1; i < nodes.size() - 1; ++i) {
            nodes.get(i).getStyleClass().add("middle");
        }
    }

 }
