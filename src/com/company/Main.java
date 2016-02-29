package com.company;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.Math.*;

import java.util.stream.Collectors;

public class Main {
    private final static byte lineType = 2;
    private final static byte pointType = 1;

    private final static Charset ENCODING = StandardCharsets.UTF_8;
    private static Long trackObjectID;
    private static List<GPS_Point> unsortFileEntries = new ArrayList<GPS_Point>();

    private static void usage() {
        System.err.println("Usage: TrackMaker <file.txt>");
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
        String[] split=inputString.split("\\s+");
        if (split.length < 8)  {
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
            String note=null;
        if (split.length > 8) {
                note = split[8];
                System.out.println("Name is : " + objID + ", pointNum : " + pointNumber + ", lat " + lat + ", lon " + lon + ", note " + note);
            } else System.out.println("Name is : " + objID + ", pointNum : " + pointNumber + ", lat " + lat + ", lon " + lon);
    return new GPS_Point(objID, pointNumber, x, y, lat, lon, elevation, note);

    }

    static void readTextFile(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);

                Scanner fileScanner = new Scanner(path, ENCODING.name());
                int i = 0;
                while(fileScanner.hasNextLine())

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
    static void setType(List <GPS_Point> objects) {
        Long curID = 0L;
        Integer prevEntry = null;
        for (GPS_Point curObj : objects) {
           
               
                if (curID == 0) {
                    curID = curObj.getObjID();
                    prevEntry = 0;
                    continue;
                }
                else if (curID == curObj.getObjID()) {
                    objects.get(prevEntry).setType(lineType);
                    curObj.setType(lineType);
                } else { if (objects.get(prevEntry).getType()==0)
                    objects.get(prevEntry).setType(pointType);
                    curID = curObj.getObjID();
                }
                prevEntry++;
               
        }
        if (objects.get(prevEntry).getType()==0) objects.get(prevEntry).setType(pointType);
    }
    
    static Double getDelta(Double x1, Double y1, Double x2, Double y2, Double xp, Double yp) {
        Double segmentSquare = pow((x2 - x1),2) + pow((y2 - y1),2);
        Double aSquare = pow((xp - x1),2) + pow((yp - y1),2);
        Double bSquare = pow((x2 - xp),2) + pow((y2 - yp),2);
        return aSquare + bSquare -segmentSquare;
    }
    
    static Integer findSegment(GPS_Point gpsPoint, List<GPS_Point> track) {
        Integer curSegment = -1;
        Double delta = Double.MAX_VALUE;
        Integer prevEntry = null;
        for (GPS_Point curObj: track) {
                if (curSegment == -1) {
                    curSegment = 1;
                    prevEntry = 0;
                    continue;
                }
                else  {
                    Double curDelta = getDelta(track.get(prevEntry).getPlainX(),track.get(prevEntry).getPlainY(), curObj.getPlainX(), curObj.getPlainY(), gpsPoint.getPlainX(), gpsPoint.getPlainY());
                    if (curDelta == 0) return prevEntry;
                    if (curDelta < delta) {
                        curSegment = prevEntry;
                        delta = curDelta;
                    }
                } 
                prevEntry++;
            
            
            
        }
        if (delta > 500) return -1;
        return curSegment;
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
        if (args.length > 0) filename = args[0];
        else usage();
        String fileURL = convertToFileURL(filename);

        try {
            readTextFile(filename);
            trackObjectID = 18L;
        }
        catch (IOException ex) {
            System.err.println("Error in reading input file!!!");
            System.exit(1);
        }

        setType(unsortFileEntries);
        List<GPS_Point> track = unsortFileEntries
                        .stream()
                        .filter(p -> p.getObjID() == trackObjectID)
                        .collect(Collectors.toList());

        System.out.println("Track:/n" + track);
    
        List<GPS_Point> points =
                unsortFileEntries
                        .stream()
                        .filter(p -> p.getType() == pointType)
                        .collect(Collectors.toList());
    
        System.out.println("Points: \n" + points);
        
        insertPointsToTrack(track, points);
    
        System.out.println("Track+points:\n" );
        track.stream()
              .forEachOrdered(System.out::println);
        
    }
}
