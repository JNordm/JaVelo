package ch.epfl.javelo.data;

import ch.epfl.javelo.projection.PointCh;
import ch.epfl.javelo.projection.SwissBounds;
import ch.epfl.test.TestRandomizer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class GraphSectorsTest {

    @Test
    void sectorsInAreaWorksOnAllSectorsWithRandomNodes(){
        ByteBuffer sectorBuffer = ByteBuffer.allocate(6*16384);
        RandomGenerator random = TestRandomizer.newRandom();
        int lastNode = 0 ;
        List<GraphSectors.Sector> fullSectorsList = new ArrayList<>();
        for (int i = 0; i < 16384; i++){
            int r = random.nextInt(500, 1000);
            sectorBuffer.putInt(lastNode);
            sectorBuffer.putShort((short)r);
            fullSectorsList.add(new GraphSectors.Sector(lastNode, lastNode + r));
            lastNode += r ;
        }
        GraphSectors graph = new GraphSectors(sectorBuffer);
        assertEquals(fullSectorsList, graph.sectorsInArea(new PointCh(SwissBounds.MIN_E, SwissBounds.MIN_N), 500000));
    }

    @Test
    void sectorsInAreaWorksOnNonTrivialSectors(){
        ByteBuffer sectorBuffer = ByteBuffer.allocate(6*16384);
        int lastNode = 0 ;
        List<GraphSectors.Sector> sectorsList = new ArrayList<>();
        for (int i = 0; i < 16384; i++){
            sectorBuffer.putInt(lastNode);
            sectorBuffer.putShort((short)1);
            lastNode++ ;
        }
        GraphSectors graph = new GraphSectors(sectorBuffer);
        PointCh point = new PointCh(14 * (SwissBounds.WIDTH / 128.0) + 400 + SwissBounds.MIN_E, 68 * (SwissBounds.HEIGHT / 128.0) + 400 + SwissBounds.MIN_N);
        for (int j = 8332; j <= 9100; j += 128){
            for (int i = j; i < j + 5; i++) {
                sectorsList.add(new GraphSectors.Sector(i, i + 1));
            }
        }
       assertEquals(sectorsList, graph.sectorsInArea(point, (SwissBounds.WIDTH/128) * 2));
    }

    @Test
    void sectorsInAreaWorksWithASingleSector(){
        ByteBuffer sectorBuffer = ByteBuffer.allocate(6*16384);
        int lastNode = 0 ;
        List<GraphSectors.Sector> sectorsList = new ArrayList<>();
        for (int i = 0; i < 16384; i++){
            sectorBuffer.putInt(lastNode);
            sectorBuffer.putShort((short)1);
            lastNode++ ;
        }
        GraphSectors graph = new GraphSectors(sectorBuffer);
        sectorsList.add(new GraphSectors.Sector(8590, 8591));
        assertEquals(sectorsList, graph.sectorsInArea(new PointCh(14 * (SwissBounds.WIDTH / 128.0) + 1400 + SwissBounds.MIN_E,67 * (SwissBounds.HEIGHT / 128.0) + 800 + SwissBounds.MIN_N), 200));
    }

    @Test
    void sectorsInAreaWorksWithExtremeSectors(){
        ByteBuffer sectorBuffer = ByteBuffer.allocate(6*16384);
        int lastNode = 0 ;
        for (int i = 0; i < 16384; i++){
            sectorBuffer.putInt(lastNode);
            sectorBuffer.putShort((short)1);
            lastNode++ ;
        }
        GraphSectors graph = new GraphSectors(sectorBuffer);

        List<GraphSectors.Sector> sectorsList1 = new ArrayList<>();
        List<GraphSectors.Sector> sectorsList2 = new ArrayList<>();
        List<GraphSectors.Sector> sectorsList3 = new ArrayList<>();

        sectorsList1.add(new GraphSectors.Sector(0, 1));
        PointCh point1 = new PointCh(SwissBounds.MIN_E, SwissBounds.MIN_N);

        sectorsList2.add(new GraphSectors.Sector(16383, 16384));
        PointCh point2 = new PointCh(SwissBounds.MAX_E, SwissBounds.MAX_N);

        sectorsList3.add(new GraphSectors.Sector(16254,16255));
        sectorsList3.add(new GraphSectors.Sector(16255,16256));
        sectorsList3.add(new GraphSectors.Sector(16382,16383));
        sectorsList3.add(new GraphSectors.Sector(16383,16384));
        PointCh point3 = new PointCh((SwissBounds.MAX_E), SwissBounds.MAX_N);

        assertEquals(sectorsList1, graph.sectorsInArea(point1, 300));
        assertEquals(sectorsList2, graph.sectorsInArea(point2, 350));
        assertEquals(sectorsList3, graph.sectorsInArea(point3, 2900));
    }

    @Test
    void sectorsInAreaWorkOnAllSectorsA() {
        //création d'un Buffer rempli aléatoirement
        ByteBuffer graphSector = ByteBuffer.allocate(98304);
        int a = 0;
        List<GraphSectors.Sector> expectedGS = new ArrayList<GraphSectors.Sector>();

        for (int i = 0; i < 16384; i++) {
            graphSector.putInt(a);
            int b = (int) Math.round(Math.random()*10);
            a +=b;
            graphSector.putShort((short)b);
            expectedGS.add(new GraphSectors.Sector(a - b, a));
        }
        GraphSectors gss = new GraphSectors(graphSector);
        List<GraphSectors.Sector> actualSector = gss.sectorsInArea(new PointCh(2500000, 1100000),1000000);
        assertEquals(expectedGS, actualSector); ;
    }

    @Test
    void sectorsInAreaWorkOnOneSectorsA() {
        //création d'un Buffer rempli aléatoirement
        ByteBuffer graphSector = ByteBuffer.allocate(98304);
        int a = 0;
        List<GraphSectors.Sector> expectedGS = new ArrayList<GraphSectors.Sector>();

        for (int i = 0; i < 16384; i++) {
            graphSector.putInt(a);
            int b = (int) Math.round(Math.random()*10);
            a +=b;
            graphSector.putShort((short)b);
            if (i == 1797) expectedGS.add(new GraphSectors.Sector(a - b, a));
        }
        GraphSectors gss = new GraphSectors(graphSector);
        List<GraphSectors.Sector> actualSector = gss.sectorsInArea(new PointCh(2500000, 1100000),10);
        assertEquals(expectedGS, actualSector); ;
    }

    @Test
    void sectorsInAreaWorkInvalideSectorsA() {
        //création d'un Buffer rempli aléatoirement
        ByteBuffer graphSector = ByteBuffer.allocate(98304);
        int a = 0;
        List<GraphSectors.Sector> expectedGS = new ArrayList<GraphSectors.Sector>();

        for (int i = 0; i < 16384; i++) {
            graphSector.putInt(a);
            int b = (int) Math.round(Math.random()*10);
            a +=b;
            graphSector.putShort((short)b);
            if (i == 1798) expectedGS.add(new GraphSectors.Sector(a - b, a));
        }
        GraphSectors gss = new GraphSectors(graphSector);
        List<GraphSectors.Sector> actualSector = gss.sectorsInArea(new PointCh(2500000, 1100000),10);
        assertEquals(false, (expectedGS.equals(actualSector)));
    }

    @Test
    void sectorsInAreaWorkOnSomeSectorsA() {
        //création d'un Buffer rempli aléatoirement
        ByteBuffer graphSector = ByteBuffer.allocate(98304);
        int a = 0;
        List<GraphSectors.Sector> expectedGS = new ArrayList<GraphSectors.Sector>();

        for (int i = 0; i < 16384; i++) {
            graphSector.putInt(a);
            int b = (int) Math.round(Math.random()*10);
            a +=b;
            graphSector.putShort((short)b);
            if (i == 1797) expectedGS.add(new GraphSectors.Sector(a - b, a));
        }
        GraphSectors gss = new GraphSectors(graphSector);
        List<GraphSectors.Sector> actualSector = gss.sectorsInArea(new PointCh(2500000, 1100000),10);
        assertEquals(expectedGS, actualSector); ;
    }

    @Test
    void sectorsInAreaWorkOnUpperRightHandSectorsA() {
        //création d'un Buffer rempli aléatoirement
        ByteBuffer graphSector = ByteBuffer.allocate(98304);
        int a = 0;
        List<GraphSectors.Sector> expectedGS = new ArrayList<GraphSectors.Sector>();

        for (int i = 0; i < 16384; i++) {
            graphSector.putInt(a);
            int b = (int) Math.round(Math.random()*10);
            a +=b;
            graphSector.putShort((short)b);
            switch (i) {
                case 16126, 16127, 16254, 16255, 16382, 16383 :
                    expectedGS.add(new GraphSectors.Sector(a - b, a));
                    break;
            }
        }
        GraphSectors gss = new GraphSectors(graphSector);
        List<GraphSectors.Sector> actualSector = gss.sectorsInArea(new PointCh(2_832_500, 1_294_200),2730);
        assertEquals(expectedGS, actualSector);
        System.out.println();
    }

    @Test
    void sectorsInAreaWorksWithExtremeSectorsA(){
        ByteBuffer sectorBuffer = ByteBuffer.allocate(6*16384);
        int lastNode = 0 ;
        for (int i = 0; i < 16384; i++){
            sectorBuffer.putInt(lastNode);
            sectorBuffer.putShort((short)1);
            lastNode++ ;
        }
        GraphSectors graph = new GraphSectors(sectorBuffer);

        List<GraphSectors.Sector> sectorsList1 = new ArrayList<>();
        List<GraphSectors.Sector> sectorsList2 = new ArrayList<>();
        List<GraphSectors.Sector> sectorsList3 = new ArrayList<>();

        sectorsList1.add(new GraphSectors.Sector(0, 1));
        PointCh point1 = new PointCh(SwissBounds.MIN_E, SwissBounds.MIN_N);

        sectorsList2.add(new GraphSectors.Sector(16383, 16384));
        PointCh point2 = new PointCh(SwissBounds.MAX_E, SwissBounds.MAX_N);

        sectorsList3.add(new GraphSectors.Sector(16254,16255));
        sectorsList3.add(new GraphSectors.Sector(16255,16256));
        sectorsList3.add(new GraphSectors.Sector(16382,16383));
        sectorsList3.add(new GraphSectors.Sector(16383,16384));
        PointCh point3 = new PointCh((SwissBounds.MAX_E), SwissBounds.MAX_N);


        assertEquals(sectorsList1, graph.sectorsInArea(point1, 300));
        assertEquals(sectorsList2, graph.sectorsInArea(point2, 350));
        assertEquals(sectorsList3, graph.sectorsInArea(point3, 2900));
    }
}
