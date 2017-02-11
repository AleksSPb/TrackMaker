package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

import static java.lang.Math.*;

import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;


public class Main {
    private final static byte LINE_TYPE = 2;
    private final static byte POINT_TYPE = 1;
    private final static Charset ENCODING = StandardCharsets.UTF_8;

    private static Double lowSpeed = 0.5;//metres in second
    private static Double speedRandomRange = 0.25;//metres in second
    private static Double beforePointLatencyLow = 22.0;//second
    private static Double beforePointRandomRange = 18.0;//second
    private static Double afterPointLatencyLow = 15.0;//second
    private static Double afterPointRandomRange = 15.0;//second
    private static LocalDateTime trackStartTime;
    private final static DateFormat gpxDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static Long trackObjectID;
    private static List<GPS_Point> unsortFileEntries = new ArrayList<GPS_Point>();
    private static StringBuilder trackText = new StringBuilder();
    private static StringBuilder wayPointsText = new StringBuilder();
    private static StringBuilder wayPointsDescr = new StringBuilder();
    private static long startTime = 0;
    private static int wptNumber = 1;

    private static void usage() {
        System.err.println("Usage: TrackMaker <file.txt> track_ID startTime firstWPTNumber lowestSpeed");
        System.err.println("Input text file structure:");
        System.err.println("ObjID   PointNum X Y Lat Lon H Note");
        System.err.println("Output text file (_track.txt) structure:");
        System.err.println("ObjID   PointNum Lat Lon Note H distance_from_previsios_point time_from_previsiuos_point");
        System.exit(1);
    }

    private static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    static GPS_Point createGPSObjectFromString(String inputString) throws Exception {
        String[] split = inputString.split("\\s+");
        if (split.length < 8) {
            System.out.println("Empty or invalid line. Unable to process.");
            throw new Exception("Error! Line has too less arguments");
        }
        //assumes the line has a certain structure
//            String str1 = integerValue scanner.next().;
        Long objID = Long.valueOf(split[0]);
        Long pointNumber = Long.valueOf(split[2]);
        Double x = Double.valueOf(split[3]);
        Double y = Double.valueOf(split[4]);
        Double lat = Double.valueOf(split[5]);
        Double lon = Double.valueOf(split[6]);
        Double elevation = Double.valueOf(split[7]);
        String note = null;
        if (split.length > 8) {
            note = split[8];
            //   System.out.println("Name is : " + objID + ", pointNum : " + pointNumber + ", lat " + lat + ", lon " + lon + ", note " + note);
        } //else System.out.println("Name is : " + objID + ", pointNum : " + pointNumber + ", lat " + lat + ", lon " + lon);
        return new GPS_Point(objID, pointNumber, x, y, lat, lon, elevation, note);

    }

    static void readTextFile(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);

        Scanner fileScanner = new Scanner(path, "windows-1251");
        int i = 0;
        while (fileScanner.hasNextLine())

        {
            //process each line in some way
            try {
                unsortFileEntries.add(createGPSObjectFromString(fileScanner.nextLine()));
                i++;
            } catch (Exception ex) {
                System.out.println("Problem with line " + i++);
            }
        }


    }

    static void setType(List<GPS_Point> objects) {
        Long curID = 0L;
        Integer prevEntry = null;
        for (GPS_Point curObj : objects) {


            if (curID == 0) {
                curID = curObj.getObjID();
                prevEntry = 0;
                continue;
            } else if (curID == curObj.getObjID()) {
                objects.get(prevEntry).setType(LINE_TYPE);
                curObj.setType(LINE_TYPE);
            } else {
                if (objects.get(prevEntry).getType() == 0)
                    objects.get(prevEntry).setType(POINT_TYPE);
                curID = curObj.getObjID();
            }
            prevEntry++;

        }
        if (objects.get(prevEntry).getType() == 0) objects.get(prevEntry).setType(POINT_TYPE);
    }

    static Double getDelta(Double x1, Double y1, Double x2, Double y2, Double xp, Double yp) {
        Double segmentSquare = pow((x2 - x1), 2) + pow((y2 - y1), 2);
        Double aSquare = pow((xp - x1), 2) + pow((yp - y1), 2);
        Double bSquare = pow((x2 - xp), 2) + pow((y2 - yp), 2);
        return aSquare + bSquare - segmentSquare;
    }

    static Integer findSegment(GPS_Point gpsPoint, List<GPS_Point> track) {
        Integer curSegment = -1;
        Double delta = Double.MAX_VALUE;
        Integer prevEntry = null;
        for (GPS_Point curObj : track) {
            if (curSegment == -1) {
                curSegment = 1;
                prevEntry = 0;
                continue;
            } else {
                Double curDelta = getDelta(track.get(prevEntry).getPlainX(), track.get(prevEntry).getPlainY(), curObj.getPlainX(), curObj.getPlainY(), gpsPoint.getPlainX(), gpsPoint.getPlainY());
                if (curDelta == 0) return prevEntry;
                if (curDelta < delta) {
                    curSegment = prevEntry;
                    delta = curDelta;
                }
            }
            prevEntry++;


        }
        if (delta > 500) return -1;
        return curSegment + 1;
    }

    static void insertPointsToTrack(List<GPS_Point> track, List<GPS_Point> points) {
        for (GPS_Point curObj : points) {
            Integer insertKey = findSegment(curObj, track);
            if (insertKey >= 0) {
                track.add(insertKey, curObj);
            } else {
                System.out.println("Point not near track!!! Point " + curObj.getObjID() + " note:" + curObj.getNote());
            }
        }
    }

    public static void main(String[] args) {
        // write your code here
        String filename = null;
        trackObjectID = 18L;
        if (args.length > 2) {
            filename = args[0];
            trackObjectID = Long.parseLong(args[1]);

                try {
                    if (args.length>3) {
                    Integer startWPTNumber = Integer.parseInt(args[3]);
                    wptNumber = startWPTNumber;}
                    if (args.length>4) {
                    Double lowestSpeed =  Double.parseDouble(args[4]);
                    lowSpeed = lowestSpeed;}
                    
                } catch (NumberFormatException ex) {
                    System.err.println("Third parameter must be number. Error in parsing " + args[2]);
                }
            try {
                Date date = gpxDateFormat.parse(args[2]);
                startTime = date.getTime();
                System.out.println(gpxDateFormat.format(date) + "  getTime - " + date.getTime());
            } catch (Exception ex) {
                System.out.println("Unparsing date!");
                System.exit(1);
            }
            
    
        }

        else usage();

        try {
            readTextFile(filename);

        } catch (IOException ex) {
            System.err.println("Error in reading input file!!!");
            System.exit(1);
        }
        System.out.println("Try to find line with ID " + trackObjectID);
        setType(unsortFileEntries);
        List<GPS_Point> track = unsortFileEntries
                .stream()
                .filter(p -> p.getObjID() == trackObjectID)
                .collect(Collectors.toList());

        //   System.out.println("Track:/n" + track);

        List<GPS_Point> points =
                unsortFileEntries
                        .stream()
                        .filter(p -> p.getType() == POINT_TYPE)
                        .collect(Collectors.toList());

        // System.out.println("Points: \n" + points);

        insertPointsToTrack(track, points);
    
  /*      System.out.println("Track+points:\n" );
        track.stream()
              .forEachOrdered(System.out::println);*/
  trackText.append("<trk>\n" +
          "    <name>").append(FilenameUtils.removeExtension(FilenameUtils.getName(filename))).append("</name>\n" +
          "    <extensions>\n" +
          "      <gpxx:TrackExtension>\n" +
          "        <gpxx:DisplayColor>Red</gpxx:DisplayColor>\n" +
          "      </gpxx:TrackExtension>\n" +
          "    </extensions>\n" +
          " <trkseg>\n");

        String trackFileName = FilenameUtils.removeExtension(filename) + "_track.txt";

        wayPointsDescr.append(FilenameUtils.removeExtension(FilenameUtils.getName(filename))).append("\n" + gpxDateFormat.format(new Date(startTime)) + "\n");

        try (PrintWriter fileout = new PrintWriter(trackFileName)) {

            Random randomGenerator = new Random();
            Double trackDuration = 0.0;
            Double trackLength = 0.0;
            fileout.println(track.get(0) + "\t" + (track.get(0).getElevation() + randomGenerator.nextInt(20) / 13 - 10 / 13.0));
            for (int i = 1; i < track.size(); i++) {
                GPS_Point prev = track.get(i - 1);
                Double distance = prev.distance(track.get(i));
                trackLength += distance;
                Double speed = lowSpeed + randomGenerator.nextDouble() * speedRandomRange;
                if (randomGenerator.nextInt(10) < 3) speed = speed - speedRandomRange / 2;//иногда ещё замедляемся.
                Double additionalTime = 0.0;
                if (track.get(i).getType() == POINT_TYPE)
                    additionalTime = beforePointLatencyLow + randomGenerator.nextDouble() * beforePointRandomRange;
                if (prev.getType() == POINT_TYPE)
                    additionalTime = afterPointLatencyLow + randomGenerator.nextDouble() * afterPointRandomRange;
                if (i>50 && i<track.size()-50) {
                    if (randomGenerator.nextInt(100) == 5) {
                        Integer addRandomTime = 120 + randomGenerator.nextInt(100);
                        additionalTime +=addRandomTime;
                        System.out.format("Add %d seconds for vertex %d\n", addRandomTime, i);
                    }
                }
                Double time = distance / speed + additionalTime;
                trackDuration += time;
                Double elevation = (track.get(i).getElevation() + randomGenerator.nextInt(20) / 49.0 - 10 / 49.0);
                fileout.println(track.get(i) + "\t" + elevation + "\t" + distance + "\t" + time);
                putToGPX(track.get(i), elevation, trackDuration);
            }
            System.out.println("Track length " + trackLength + " metres, vertexes - " + track.size() + ", duration in hours " + trackDuration / 3600);
        } catch (FileNotFoundException ex) {
            System.out.println("File not found!");
            System.exit(1);
        }
        trackText.append(" </trkseg>\n</trk>");
        String gpxFileName = FilenameUtils.removeExtension(filename) + ".gpx";

        try (PrintWriter fileout = new PrintWriter(gpxFileName)) {
            fileout.print("<?xml version=\"1.0\" encoding=\"utf-8\"?><gpx creator=\"Garmin Desktop App\" version=\"1.1\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/WaypointExtension/v1 http://www8.garmin.com/xmlschemas/WaypointExtensionv1.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/ActivityExtension/v1 http://www8.garmin.com/xmlschemas/ActivityExtensionv1.xsd http://www.garmin.com/xmlschemas/AdventuresExtensions/v1 http://www8.garmin.com/xmlschemas/AdventuresExtensionv1.xsd http://www.garmin.com/xmlschemas/PressureExtension/v1 http://www.garmin.com/xmlschemas/PressureExtensionv1.xsd http://www.garmin.com/xmlschemas/TripExtensions/v1 http://www.garmin.com/xmlschemas/TripExtensionsv1.xsd http://www.garmin.com/xmlschemas/TripMetaDataExtensions/v1 http://www.garmin.com/xmlschemas/TripMetaDataExtensionsv1.xsd http://www.garmin.com/xmlschemas/ViaPointTransportationModeExtensions/v1 http://www.garmin.com/xmlschemas/ViaPointTransportationModeExtensionsv1.xsd http://www.garmin.com/xmlschemas/CreationTimeExtension/v1 http://www.garmin.com/xmlschemas/CreationTimeExtensionsv1.xsd http://www.garmin.com/xmlschemas/AccelerationExtension/v1 http://www.garmin.com/xmlschemas/AccelerationExtensionv1.xsd http://www.garmin.com/xmlschemas/PowerExtension/v1 http://www.garmin.com/xmlschemas/PowerExtensionv1.xsd http://www.garmin.com/xmlschemas/VideoExtension/v1 http://www.garmin.com/xmlschemas/VideoExtensionv1.xsd\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:wptx1=\"http://www.garmin.com/xmlschemas/WaypointExtension/v1\" xmlns:gpxtrx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:trp=\"http://www.garmin.com/xmlschemas/TripExtensions/v1\" xmlns:adv=\"http://www.garmin.com/xmlschemas/AdventuresExtensions/v1\" xmlns:prs=\"http://www.garmin.com/xmlschemas/PressureExtension/v1\" xmlns:tmd=\"http://www.garmin.com/xmlschemas/TripMetaDataExtensions/v1\" xmlns:vptm=\"http://www.garmin.com/xmlschemas/ViaPointTransportationModeExtensions/v1\" xmlns:ctx=\"http://www.garmin.com/xmlschemas/CreationTimeExtension/v1\" xmlns:gpxacc=\"http://www.garmin.com/xmlschemas/AccelerationExtension/v1\" xmlns:gpxpx=\"http://www.garmin.com/xmlschemas/PowerExtension/v1\" xmlns:vidx1=\"http://www.garmin.com/xmlschemas/VideoExtension/v1\">\n\n" +
                    "  <metadata>\n" +
                    "    <link href=\"http://www.garmin.com\">\n" +
                    "      <text>Garmin International</text>\n" +
                    "    </link>\n" +
                    "    <time>2017-02-12T20:41:44Z</time>\n" +
                    "    <bounds maxlat=\"58.830581456422806\" maxlon=\"28.510672738775611\" minlat=\"58.75233463011682\" minlon=\"28.418036447837949\" />\n" +
                    "  </metadata>");
            fileout.println(wayPointsText.toString());
            fileout.println(trackText.toString());
            fileout.print("\n" +
                    "</gpx>");
        } catch (FileNotFoundException ex) {
            System.out.println("File not found!");
            System.exit(1);
        }
        String descriptionFileName = FilenameUtils.removeExtension(filename) + "_descr.txt";

        try (PrintWriter fileout = new PrintWriter(descriptionFileName)) {
        fileout.print(wayPointsDescr.toString());
        } catch (FileNotFoundException ex) {
            System.out.println("File not found!");
            System.exit(1);
        }

        System.out.println(wayPointsText.toString());
        System.out.println(trackText.toString());
        System.out.println(wayPointsDescr.toString());
    }

    private static void putToGPX(GPS_Point point, Double elevation, Double secondsFromFirstPoint ) {
        Date dateTime = new Date();
        Double milliseconds = secondsFromFirstPoint * 1000;
        dateTime.setTime(startTime + milliseconds.intValue());
        if (point.getType() == LINE_TYPE) {
            trackText.append(" <trkpt lat=\"").append(point.getLatitude()).append("\" lon=\"").append(point.getLongitude()).append("\">\n" +
                    "  <ele>").append(elevation).append("</ele>\n" +
                    "  <time>").append(gpxDateFormat.format(dateTime)).append("</time>\n" +
                    " </trkpt>\n");
        } else if (point.getType() == POINT_TYPE) {
            wayPointsDescr.append(wptNumber).append("\t" + point.getNote() + "\t" + gpxDateFormat.format(dateTime) + "\n");
            wayPointsText.append("<wpt lat=\"").append(point.getLatitude()).append("\" lon=\"").append(point.getLongitude()).append("\">\n" +
                    "  <ele>").append(elevation).append("</ele>\n" +
                    "  <time>").append(gpxDateFormat.format(dateTime)).append("</time>\n" +
                    "  <name>").append(wptNumber++).append("</name> \n" +
                    "  <sym>Waypoint</sym> \n" +
                    "  <type>user</type>\n" +
                    "  <extensions>\n" +
                    "   <gpxx:WaypointExtension>\n" +
                    "    <gpxx:DisplayMode>SymbolAndName</gpxx:DisplayMode>\n" +
                    "    </gpxx:WaypointExtension>\n" +
                    "  </extensions>\n" +
                    " </wpt>");
        }

    }
}
