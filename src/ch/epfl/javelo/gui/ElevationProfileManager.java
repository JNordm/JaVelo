package ch.epfl.javelo.gui;

import ch.epfl.javelo.routing.ElevationProfile;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 10.3.1
 * ElevationProfileManager
 * <p>
 * Classe gérant l'affichage et l'interaction avec le profil en long d'un itinéraire.
 *
 * @author Jean Nordmann (344692)
 * @author Maxime Ducourau (329544)
 */

public final class ElevationProfileManager {

    //Constante représentant le nombre de mètres dans un kilomètre.
    private static final int NUMBER_OF_METERS_IN_A_KILOMETER = 1000;

    //Constante représentant la taille de la police souhaitée.
    private static final int FONT_SIZE = 10;

    //Constante représentant la police utilisée pour tous les textes.
    private static final String FONT = "Avenir";

    /**
     * Attribut représentant une propriété, accessible en lecture seule,
     * contenant le profil à afficher.
     */
    private final ReadOnlyObjectProperty<ElevationProfile> elevationProfile;

    /**
     * Propriété, accessible en lecture seule, contenant la position le long
     * du profil à mettre en évidence.
     */
    private final ReadOnlyDoubleProperty highlightedPosition;

    /**
     * Attribut représentant le panneau contenant le dessin du profil.
     */
    private final BorderPane borderPane;

    /**
     * Attribut représentant le panneau dans lequel sont placés le chemin représentant la grille,
     * un certain nombre d'étiquettes textuelles regroupées dans un groupe et représentant les
     * étiquettes de la grille, un polygone représentant le graphe du profil, une ligne
     * représentant la position mise en évidence.
     */
    private final Pane pane;

    /**
     * Attribut représentant un panneau de type VBox contenant un texte contenant les statistiques
     * du profil.
     */
    private VBox vBox;

    /**
     * Attribut de type Path représentant la grille.
     */
    private Path path;

    /**
     * Attribut représentant le groupe contenant les étiquettes de la grille.
     */
    private Group group;

    /**
     * Attribut de type Polygon représentant le graphe du profil.
     */
    private Polygon polygon;

    /**
     * Attribut de type Line représentant la position mise en évidence.
     */
    private Line line;

    /**
     * Attribut de type Text représentant les statistiques du profil.
     */
    private Text statisticsText;

    /**
     * Attribut représentant une propriété JavaFX contenant la position de la souris sur le profil.
     */
    private DoubleProperty mousePositionOnProfile;

    /**
     * Attribut représentant une propriété JavaFX contenant le rectangle en deux dimensions dans
     * lequel doit être dessinée le profil.
     */
    private ObjectProperty<Rectangle2D> rectangle2D;

    /**
     * Attribut représentant une propriété JavaFX contenant la transformation passant des
     * coordonnées du panneau JavaFX contenant le rectangle bleu au système de coordonnées du
     * "monde réel".
     */
    private ObjectProperty<Transform> screenToWorldTransform;

    /**
     * Attribut représentant une propriété JavaFX contenant la transformation inverse.
     */
    private ObjectProperty<Transform> worldToScreenTransform;

    /**
     * Attribut de type Insets, représentant les distances entre les bords du panneau et les
     * bords du rectangle bleu, en pixels (dans l'ordre : haut, droite, bas, gauche).
     */
    private final Insets insets = new Insets(10, 10, 20, 40);

    /**
     * Attribut représentant un tableau contenant les valeurs possibles pour l'écart entre les
     * lignes verticales correspondant à la position (en mètres).
     */
    private final int[] POS_STEPS = {1000, 2000, 5000, 10_000, 25_000, 50_000, 100_000};

    /**
     * Attribut représentant un tableau contenant les valeurs possibles pour l'écart entre les
     * lignes horizontales correspondant à l'altitude (en mètres).
     */
    private final int[] ELE_STEPS = {5, 10, 20, 25, 50, 100, 200, 250, 500, 1_000};

    /**
     * Attribut représentant la valeur minimale pour l'écart entre deux lignes verticales, en
     * pixels JavaFX.
     */
    private static final double MIN_PIXEL_POS = 50;

    /**
     * Attribut représentant la valeur minimale pour l'écart entre deux lignes horizontales, en
     * pixels JavaFX.
     */
    private static final double MIN_PIXEL_ELE = 25;

    /**
     * Constructeur initialisant tous les attributs à leurs valeurs passées en paramètres, ou à
     * leur valeur par défaut, liant les valeurs de certains attributs à d'autres (rectangle
     * bleu et ligne de mise en évidence) et configurant les auditeurs et les gestionnaires
     * d'évènements.
     * @param elevationProfile propriété, accessible en lecture seule, contenant le profil à
     *                         afficher.
     * @param highlightedPosition propriété, accessible en lecture seule, contenant la position
     *                            le long du profil à mettre en évidence.
     */
    public ElevationProfileManager(ReadOnlyObjectProperty<ElevationProfile> elevationProfile,
                                   ReadOnlyDoubleProperty highlightedPosition) {
        this.elevationProfile = elevationProfile;
        this.highlightedPosition = highlightedPosition;
        pane = new Pane();
        borderPane = new BorderPane();

        //Initialise tous les objets non finaux de la classe.
        createObject();

        //Configure le grand panneau et indiquant l'emplacement de chaque panneau secondaire dans
        //le panneau général.
        borderPane.setCenter(pane);
        borderPane.setBottom(vBox);

        //Ajoute la feuille de style CSS à attacher au nœud.
        borderPane.getStylesheets().add("elevation_profile.css");

        //Lie les dimensions du "petit" panneau aux dimensions du panneau "général".
        pane.prefHeightProperty().bind(borderPane.heightProperty());
        pane.prefWidthProperty().bind(borderPane.widthProperty());

        //Lie les coordonnées du rectangle bleu, et celles de la ligne de mise en évidence.
        bindBlueRectangleDimensions();
        bindHighlightedLine();

        //Configure l'affichage du profil, les auditeurs et les gestionnaires d'évènements.
        setUpListener();
        setUpEventHandlers();

        //Calcule les nouvelles statistiques du profil.
        vBox.getChildren().clear();
        vBox.getChildren().add(statisticsText);
    }

    /**
     * Méthode publique retournant le panneau de la classe.
     * @return Le panneau contenant le dessin du profil, de type Pane.
     */
    public BorderPane pane() {
        return borderPane;
    }

    /**
     * Méthode publique retournant la propriété en lecture seule contenant la position de la souris
     * sur le profil.
     * @return la propriété en lecture seule contenant la position de la souris sur le profil.
     */
    public ReadOnlyDoubleProperty mousePositionOnProfileProperty() {
        return mousePositionOnProfile;
    }

    /**
     * Méthode privée configurant les auditeurs liés aux changements de dimensions du rectangle
     * bleu et des changements du profil.
     */
    private void setUpListener() {
        //Auditeur détectant les changements de dimensions du rectangle bleu et recalculant les
        //transformations et l'affichage du profil.
        rectangle2D.addListener(e -> setUpProfileDisplay());

        //Auditeur détectant les changements du profil et recalculant les transformations et
        //l'affichage du profil.
        elevationProfile.addListener((p, oldV, newV) -> {
            if (oldV != newV && newV != null) {
                if (elevationProfile.get() != null) setUpProfileDisplay();

                //Calcule les nouvelles statistiques du profil.
                vBox.getChildren().clear();
                formatStatistics();
                vBox.getChildren().add(statisticsText);
            }
        });

    }
    /**
     * Méthode privée configurant l'affichage du profil.
     */
    private void setUpProfileDisplay() {
        //Calcule les nouvelles transformations utiles.
        setTransformation();

        //Calcule le polygone représentant le profil.
        computePolygon();

        //Calcule la grille servant de repère au graphe du profil, ainsi que les étiquettes
        //rattachées.
        initializeGridAndLabels();

        //Ajoute tous les nouveaux nœuds au panneau gérant l'affichage du profil.
        pane.getChildren().setAll(group, path, polygon, line);
    }

    /**
     * Méthode privée calculant les positions des lignes de la grille du graphe représentant le
     * profil, ainsi que les étiquettes des valeurs le long des axes.
     */
    private void initializeGridAndLabels() {
        //Variables utilisées plusieurs fois plus bas.
        double minElevation = elevationProfile.get().minElevation();
        double maxElevation = elevationProfile.get().maxElevation();
        double elevationLength = elevationProfile.get().length();
        int spaceBetween2HLines = computeHorizontalLinesSpacing();
        int spaceBetween2VLines = computeVerticalLinesSpacing();
        int numberOfHLines = numberOfHorizontalLine();
        int numberOfVLines = numberOfVerticalLine();

        //Variable calculant le multiple de la valeur de la ligne initiale à dessiner.
        double initialHLine = Math.ceil(minElevation / spaceBetween2HLines);

        //Variable représentant la transformation passant des coordonnées du monde, à celles du
        //de JavaFX.
        Transform worldToScreen = worldToScreenTransform.get();

        List<PathElement> pathElementList = new ArrayList<>();
        List<Text> textList = new ArrayList<>();

        //Ajout de la ligne en bas du rectangle bleu si elle ne nécessite pas d'étiquette.
        if (minElevation % spaceBetween2HLines != 0) {
            addLineToPathElementList(0, minElevation, elevationLength, minElevation,
                    worldToScreen, pathElementList);
        } else {
            //S'il s'agit d'un multiple de l'espacement, alors la ligne est dessinée, et donc la
            //ligne initiale de la boucle for suivante commence à la ligne suivante.
            ++initialHLine;
        }

        //Ajout de la ligne à droite du rectangle bleu si elle ne nécessite pas d'étiquette.
        if (elevationLength % spaceBetween2VLines != 0) {
            addLineToPathElementList(elevationLength, minElevation, elevationLength, maxElevation,
                    worldToScreen, pathElementList);
        }

        for (int i = 0; i < numberOfHLines; i++) {
            double variable = (initialHLine + i) * spaceBetween2HLines;
            Point2D point2DMoveTo = addLineToPathElementList(0, variable, elevationLength,
                    variable, worldToScreen, pathElementList);
            Text text = new Text(Integer.toString((int) variable));
            setUpLabel(text, point2DMoveTo, VPos.CENTER, text.prefWidth(0) + 2,
                    "vertical");
            textList.add(text);
        }

        for (int i = 0; i < numberOfVLines; i++) {
            double variable = i * spaceBetween2VLines;
            Point2D point2DMoveTo = addLineToPathElementList(variable, minElevation, variable,
                    maxElevation, worldToScreen, pathElementList);
            Text text =
                    new Text(Integer.toString((int) variable / NUMBER_OF_METERS_IN_A_KILOMETER));
            setUpLabel(text, point2DMoveTo, VPos.TOP, text.prefWidth(0) / 2, "horizontal");
            textList.add(text);
        }

        group.getChildren().setAll(textList);
        path.getElements().setAll(pathElementList);
    }

    /**
     * Méthode privée ajoutant une ligne à la liste de PathElement donnée avec les coordonnées
     * passées en paramètres, ainsi qu'avec la transformation.
     * @param moveToX Coordonnée X du point de départ de la ligne.
     * @param moveToY Coordonnée Y du point de départ de ligne.
     * @param lineToX Coordonnée X du point d'arrivée de la ligne
     * @param lineToY Coordonnée Y du point d'arrivée de la ligne.
     * @param worldToScreen Transformation calculant les coordonnées sur l'écran à partir de
     *                      celles dans le monde réel.
     * @param pathElementList Liste des PathElements construits avec toutes les lignes de la
     *                        grille.
     * @return Le point en deux dimensions à partir duquel la ligne est tracée.
     */
    private Point2D addLineToPathElementList(double moveToX, double moveToY, double lineToX,
                                          double lineToY, Transform worldToScreen,
                                          List<PathElement> pathElementList) {
        Point2D point2DMoveTo = worldToScreen.transform(moveToX, moveToY);
        Point2D point2DLineTo = worldToScreen.transform(lineToX, lineToY);
        pathElementList.add(new MoveTo(point2DMoveTo.getX(), point2DMoveTo.getY()));
        pathElementList.add(new LineTo(point2DLineTo.getX(), point2DLineTo.getY()));
        return point2DMoveTo;
    }


    /**
     * Méthode privée permettant de configurer une étiquette et son texte.
     * @param text Texte donné.
     * @param point2DMoveTo Emplacement du texte donné sous forme de Point2D.
     * @param textOrigin Position verticale donnée.
     * @param textPrefWidth Décalage du texte par rapport au point donné en Point2D.
     * @param orientation Chaîne de caractère liée à la feuille de style du texte.
     */
    private void setUpLabel(Text text, Point2D point2DMoveTo, VPos textOrigin, double textPrefWidth,
                            String orientation) {
        text.setTextOrigin(textOrigin);
        text.setFont(Font.font(FONT, FONT_SIZE));
        text.setLayoutX(point2DMoveTo.getX() - textPrefWidth);
        text.setLayoutY(point2DMoveTo.getY());
        text.getStyleClass().add("grid_label");
        text.getStyleClass().add(orientation);
    }

    /**
     * Méthode privée liant quatre propriétés de la ligne. Sa position à la position de la souris
     * sur le profil, son ordonnée de départ et de fin aux coordonnées minimales et maximales du
     * rectangle bleu, et si elle est visible ou non en fonction de sa valeur (positive ou non).
     */
    private void bindHighlightedLine() {
        line.layoutXProperty().bind(Bindings.createDoubleBinding(() -> worldToScreenTransform.get()
                .transform(highlightedPosition.get(), 0).getX(),
                 highlightedPosition, worldToScreenTransform));
        line.startYProperty().bind(Bindings.select(rectangle2D, "minY"));
        line.endYProperty().bind(Bindings.select(rectangle2D, "maxY"));
        line.visibleProperty().bind(highlightedPosition.greaterThanOrEqualTo(0));
    }

    /**
     * Méthode privée configurant les gestionnaires d'évènements, pour obtenir la coordonnée X de
     * la souris si elle se situe sur le rectangle bleu, et sinon à NaN, lorsqu'elle sort du
     * rectangle bleu ou du panneau.
     */
    private void setUpEventHandlers() {

        //Gestionnaire d'évènements concernant le cas où la souris bouge.
        pane.setOnMouseMoved(event -> {
            //Si la souris est dans le rectangle bleu.
            if (isInBlueRectangle(event.getX(), event.getY())) {
                Point2D worldCoordinates = screenToWorldTransform.get().transform(event.getX(),
                        event.getY());
                mousePositionOnProfile.set(Math.round(worldCoordinates.getX()));
            } else {
                //Si elle n'est pas dans le rectangle bleu, sa position sur le profil est mise à
                //une valeur NaN.
                mousePositionOnProfile.set(Double.NaN);
            }
        });

        //Gestionnaire d'évènements concernant le cas où la souris quitte le panneau.
        pane.setOnMouseExited(event -> mousePositionOnProfile.set(Double.NaN));
    }


    /**
     * Méthode privée determinant si une paire de coordonnées x et y donnés en paramètres sont
     * situés dans le rectangle bleu contenant le profil.
     * @param x Coordonnée x donnée.
     * @param y Coordonnée y donnée.
     * @return Vrai si et seulement si la paire de coordonnée est dans le rectangle bleu.
     */
    private boolean isInBlueRectangle(double x, double y) {
        return rectangle2D.get().contains(x, y);
    }

    /**
     * Méthode privée calculant le polygone représentant le profil.
     */
    private void computePolygon() {
        //Le point du polygone à la coordonnée (0,0) est le coin haut gauche.
        List<Double> coordinate = new LinkedList<>();
        //Coordonnées des points de l'itinéraire.
        for (int i = 0; i <= (int) rectangle2D.get().getWidth(); i++) {
            double xOnScreen = insets.getLeft() + i;
            double xOnWorld = screenToWorldTransform.get().transform(xOnScreen, 0).getX();
            Point2D wayPointOnScreen = worldToScreenTransform.get().transform(xOnWorld,
                    elevationProfile.get().elevationAt(xOnWorld));
            coordinate.add(wayPointOnScreen.getX());
            coordinate.add(wayPointOnScreen.getY());
        }
        //Coordonnées des deux coins du bas.
        coordinate.add(rectangle2D.get().getMaxX());
        coordinate.add(rectangle2D.get().getMaxY());
        coordinate.add(rectangle2D.get().getMinX());
        coordinate.add(rectangle2D.get().getMaxY());

        polygon.getPoints().setAll(coordinate);
    }

    /**
     * Méthode privée liant les dimensions du rectangle bleu aux dimensions du panneau dans
     * lequel le rectangle bleu se trouve.
     */
    private void bindBlueRectangleDimensions() {
        rectangle2D.bind(Bindings.createObjectBinding(() -> {
            //Si les dimensions du panneau sont suffisantes pour accueillir un rectangle bleu,
            //alors on le renvoie.

            if(pane.getWidth() >= insets.getLeft() + insets.getRight()
                    && pane.getHeight() >= insets.getTop() + insets.getBottom()) {

                return new Rectangle2D(insets.getLeft(), insets.getTop(),
                        pane().getWidth() - insets.getRight() - insets.getLeft(),
                        pane.getHeight() - insets.getBottom() - insets.getTop());

            }
            //Sinon, on renvoie un rectangle de dimensions nulles.
            return new Rectangle2D(0, 0, 0, 0);
        }, pane.widthProperty(), pane.heightProperty()));
    }

    /**
     * Méthode privée mettant les statistiques du profil selon un certain format, de sorte qu'elles
     * s'affichent en ligne en dessous du profil.
     */
    private void formatStatistics() {
        statisticsText.setText(String.format("Longueur : %.1f km"
                        + "     Montée : %.0f m"
                        + "     Descente : %.0f m"
                        + "     Altitude : de %.0f m à %.0f m",
                elevationProfile.get().length() / NUMBER_OF_METERS_IN_A_KILOMETER,
                elevationProfile.get().totalAscent(),
                elevationProfile.get().totalDescent(),
                elevationProfile.get().minElevation(),
                elevationProfile.get().maxElevation()));
    }

    /**
     * Méthode privée nous permettant de calculer l'espacement des lignes verticales,
     * c'est-à-dire l'espacement entre deux indications de distance.
     * @return L'espacement des lignes verticales.
     */
    private int computeVerticalLinesSpacing() {
        for (int posStep : POS_STEPS) {
            double minPixel = rectangle2D.get().getWidth()
                   / (elevationProfile.get().length() / (double) posStep);
            //Teste si on respecte la condition pour le l'espacement actuel.
            if (minPixel >= MIN_PIXEL_POS) {
                return posStep;
            }
        }
        //Retourne l'espacement maximal si aucun ne satisfait la condition.
        return POS_STEPS[POS_STEPS.length - 1];
    }

    /**
     * Méthode privée nous permettant de calculer l'espacement des lignes horizontales,
     * c'est-à-dire l'espacement entre deux indications d'altitude.
     * @return L'espacement des lignes horizontales.
     */
    private int computeHorizontalLinesSpacing() {
        for (int eleStep : ELE_STEPS) {
            double minPixel = rectangle2D.get().getHeight()
                    / ((elevationProfile.get().maxElevation() - elevationProfile.get().minElevation())
                    / (double) eleStep);
            //Teste si on respecte la condition pour le l'espacement actuel.
            if (minPixel >= MIN_PIXEL_ELE) {
                return eleStep;
            }
        }
        //Retourne l'espacement maximal si aucun ne satisfait la condition.
        return ELE_STEPS[ELE_STEPS.length - 1];
    }

    /**
     * Méthode privée retournant le nombre de lignes horizontales ayant un texte d'altitude associé.
     * C'est-à-dire incluant la première si l'altitude minimale est un multiple de l'espacement
     * et incluant également la dernière si l'altitude maximale est un multiple de l'espacement.
     * @return Le nombre de lignes horizontales ayant un texte d'altitude associé.
     */
    private int numberOfHorizontalLine() {
        double minEle = elevationProfile.get().minElevation();
        double maxEle = elevationProfile.get().maxElevation();
        int step = computeHorizontalLinesSpacing();
        //Dénivelé au-dessus de la dernière ligne.
        double spaceUp = maxEle % step;
        //Dénivelé au-dessus de la ligne en dessous de la première ligne à dessiner.
        double spaceDown = minEle % step;
        double newDeltaEle = maxEle - spaceUp  - (minEle - spaceDown);
        //Cas limite où il faut dessiner la première ligne (donc une de +).
        if (spaceDown == 0) newDeltaEle += step;
        return (int) newDeltaEle / step;
    }

    /**
     * Méthode privée retournant le nombre de lignes verticales ayant un texte de distance associé.
     * C'est-à-dire incluant la ligne ayant l'indication "0" dans tous les cas
     * et incluant la dernière ligne si la longueur de l'itinéraire est un multiple de l'espacement.
     * @return Le nombre de lignes verticales ayant un texte de distance associé.
     */
    private int numberOfVerticalLine() {
        int maxPos = (int) elevationProfile.get().length();
        int step = computeVerticalLinesSpacing();
        //+ 1, car pour une distance de n il faut n + 1 séparateurs.
        return maxPos / step + 1;
    }

    /**
     * Méthode privée initialisant tous les attributs non finaux de la classe.
     */
    private void createObject() {
        polygon = new Polygon();
        polygon.setId("profile");
        polygon.setFill(Color.RED);
        path = new Path();
        path.setId("grid");
        group = new Group();
        line = new Line();
        statisticsText = new Text();
        vBox = new VBox();
        vBox.setId("profile_data");
        rectangle2D = new SimpleObjectProperty<>(new Rectangle2D(0, 0, 0, 0));
        worldToScreenTransform = new SimpleObjectProperty<>(Transform.translate(0, 0));
        screenToWorldTransform = new SimpleObjectProperty<>(Transform.translate(0, 0));
        mousePositionOnProfile = new SimpleDoubleProperty(Double.NaN);

    }

    /**
     * Méthode privée calculant la transformation passant des coordonnées du panneau JavaFX aux
     * coordonnées du monde "réel", et la transformation inverse.
     */
    private void setTransformation() {
        Affine affine = new Affine();
        //Décale au coin au gauche du rectangle.
        affine.prependTranslation(-insets.getLeft(), -insets.getTop());
        //Inverse les coordonnées de l'altitude. Puis Ajout de la hauteur

        affine.prependScale(1, -1);
        affine.prependTranslation(0, rectangle2D.get().getHeight());
        //Changement d'échelle.
        double deltaElevation = (elevationProfile.get().maxElevation() - elevationProfile.get().minElevation());
        affine.prependScale(1 / rectangle2D.get().getWidth(), 1 / rectangle2D.get().getHeight());
        affine.prependScale(elevationProfile.get().length(), deltaElevation);
        //Décalage de l'altitude.
        affine.prependTranslation(0, elevationProfile.get().minElevation());
        screenToWorldTransform.set(affine);
        //Si la transformation inverse est possible, alors elle est calculée.
        try {
            worldToScreenTransform.set(affine.createInverse());
        } catch (NonInvertibleTransformException e) {
            e.printStackTrace();
        }
    }
}
