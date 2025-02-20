package ch.epfl.javelo.routing;

import ch.epfl.javelo.Functions;
import ch.epfl.javelo.Preconditions;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;


/**
 * 4.3.3
 * ElevationProfile
 * <p>
 * Classe représentant le profil en long d'un itinéraire simple ou multiple.
 *
 * @author Jean Nordmann (344692)
 * @author Maxime Ducourau (329544)
 */

public final class ElevationProfile {

    /**
     * Attributs représentant la longueur du profil et les échantillons d'élévation.
     */

    private final double length;
    private final float[] elevationSamples;
    private final double MIN_ELE;
    private final double MAX_ELE;
    private final double TOTAL_ASCENT;
    private final double TOTAL_DESCENT;

    /**
     * Constructeur public.
     *
     * @param length Longueur du profil en mètres.
     * @param elevationSamples Tableau de float avec les différentes altitudes à équidistance horizontale.
     */

    public ElevationProfile(double length, float[] elevationSamples) {
        Preconditions.checkArgument(length > 0 && elevationSamples.length >= 2);
        this.length = length;
        this.elevationSamples = Arrays.copyOf(elevationSamples, elevationSamples.length);
        DoubleSummaryStatistics s = new DoubleSummaryStatistics();
        for (float i : elevationSamples) s.accept(i);
        // Calcul du max, min elevation et du total de monté et descente.
        MIN_ELE = s.getMin();
        MAX_ELE = s.getMax();
        double totalA = 0;
        double memoryA = elevationSamples[0];
        double deltaA;
        for (float i : elevationSamples) {
            deltaA = (double) i - memoryA;
            totalA = deltaA > 0 ? totalA + deltaA : totalA;
            memoryA = i;
        }
        TOTAL_ASCENT = totalA;

        double totalD = 0;
        double memoryD = elevationSamples[0];
        double deltaD;
        for (float i : elevationSamples) {
            deltaD = (double) i - memoryD;
            totalD = deltaD < 0 ? totalD - deltaD : totalD;
            memoryD = i;
        }
        TOTAL_DESCENT = totalD;
    }

    /**
     * Retourne la longueur du profil, en mètres.
     * @return La longueur du profil, en mètres.
     */

    public double length() {
        return length;
    }

    /**
     * Retourne l'altitude minimum du profil, en mètres.
     * @return L'altitude minimum du profil, en mètres.
     */

    public double minElevation() {
        return MIN_ELE;
    }

    /**
     * Retourne l'altitude maximum du profil, en mètres.
     * @return L'altitude maximum du profil, en mètres.
     */

    public double maxElevation() {
        return MAX_ELE;
    }

    /**
     * Retourne le dénivelé positif total du profil, en mètres.
     * @return Le dénivelé positif total du profil, en mètres.
     */

    public double totalAscent() {
        return TOTAL_ASCENT;
    }

    /**
     * Retourne le dénivelé négatif total du profil, en mètres. La valeur est toujours positive.
     * @return Le dénivelé négatif total du profil, en mètres. (valeur toujours positive)
     */

    public double totalDescent() {
       return TOTAL_DESCENT;
    }

    /**
     * Permet de savoir l'altitude d'un point dont on connait l'abscisse X.
     *
     * @param position Position x dont on aimerait connaître l'altitude.
     * @return L'altitude du profil à la position donnée, qui n'est pas forcément comprise entre 0 et la
     * longueur du profil. Le premier échantillon est retourné lorsque la position est négative,
     * le dernier lorsqu'elle est supérieure à la longueur.
     */

    public double elevationAt(double position) {
        return Functions.sampled(elevationSamples, length).applyAsDouble(position);
    }
}
