package ch.epfl.javelo.gui;
import ch.epfl.javelo.data.Graph;
import ch.epfl.javelo.projection.PointCh;
import ch.epfl.javelo.projection.PointWebMercator;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

public final class AnnotatedMapManager {

    private final StackPane pane;

    private BaseMapManager baseMapManager;

    private WaypointsManager waypointsManager;

    private RouteManager routeManager;

    private Graph graph;

    private TileManager tileManager;

    private RouteBean bean;

    private Consumer<String> consumer;

    private final ObjectProperty<MapViewParameters> mapViewParametersP;

    private DoubleProperty mousePositionOnRouteProperty;

    private ObjectProperty<Point2D> mousePosition;

    public AnnotatedMapManager(Graph graph, TileManager tileManager, RouteBean bean,
                               Consumer<String> consumer) {
        this.graph = graph;
        this.tileManager = tileManager;
        this.bean = bean;
        this.consumer = consumer;
        mapViewParametersP =
                new SimpleObjectProperty<>(new MapViewParameters(12, 543200, 370650));
        waypointsManager = new WaypointsManager(graph, mapViewParametersP, bean.waypointsProperty(), consumer);
        baseMapManager = new BaseMapManager(tileManager, waypointsManager, mapViewParametersP);
        routeManager = new RouteManager(bean, mapViewParametersP);
        pane = new StackPane(baseMapManager.pane(), waypointsManager.pane(), routeManager.pane());
        pane.getStylesheets().add("map.css");
        mousePositionOnRouteProperty = new SimpleDoubleProperty(Double.NaN);
        mousePosition = new SimpleObjectProperty<>(new Point2D(Double.NaN, Double.NaN));

        pane.setOnMouseExited(e -> mousePositionOnRouteProperty.set(Double.NaN));
        pane.setOnMouseMoved(e -> {
            System.out.println(mousePositionOnRouteProperty.get());
            System.out.println("met à jour la position dans anotade map mannager");
            mousePosition.set(new Point2D(e.getX(), e.getY()));
            System.out.println(mousePosition);
        });

        //mousePositionOnRouteProperty.bindBidirectional(bean.highlightedPositionProperty());

        mousePositionOnRouteProperty.addListener(e -> System.out.println(mousePositionOnRouteProperty.get()));
        mousePositionOnRouteProperty.bind(Bindings.createDoubleBinding(() -> {
            if(bean.getRoute() == null) return Double.NaN ;
            //todo A CLEAN
            PointCh mousePointCh = mapViewParametersP.get().
                    pointAt(mousePosition.get().getX(), mousePosition.get().getY())
                    .toPointCh();
            PointCh closestMousePointCh = bean.getRoute().pointClosestTo(mousePointCh).point();
            PointWebMercator closestMousePWM = PointWebMercator.ofPointCh(closestMousePointCh);
            Point2D closestMousePoint2D = new Point2D(mapViewParametersP.get().viewX(closestMousePWM),
                    mapViewParametersP.get().viewY(closestMousePWM));
            System.out.println("a bind mousePositionOnRouteProperty à la position sur la route");
            System.out.println(mousePosition.get().distance(closestMousePoint2D));
            return mousePosition.get().distance(closestMousePoint2D) <= 15
                    ? bean.getRoute().pointClosestTo(closestMousePointCh).position()
                    : Double.NaN;
        }, mousePosition));

    }

    public StackPane pane() {
        return pane;
    }

    public DoubleProperty mousePositionOnRouteProperty() {
        return mousePositionOnRouteProperty;
    }

}
