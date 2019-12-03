package org.ucc.insight.DCMST;

import org.chocosolver.util.tools.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Random;

public class DCMSTFileReader {

    private int n;
    private int[] dMax;
    private int[][] dist;
    private int lb, ub;
    private String instance;

    public DCMSTWrapper generateDCMSTWrapper() {
        return new DCMSTWrapper(n, dMax, dist, lb, ub, instance);
    }

    public DCMSTFileReader(String dir, String inst) {
        parse_CHOCO_DR(new File(dir + "/" + inst));
        instance = inst;
    }

    public DCMSTFileReader(String dir, String inst, FileType fileType) {
        File file = new File(dir + "/" + inst);
        if (fileType == FileType.CHOCO_DR) {
            parse_CHOCO_DR(file);
        } else if (fileType == FileType.ANDINST) {
            parse_ANDINST(file);
        } else if (fileType == FileType.DR_LH) {
            parse_DR_LH(file);
        } else if (fileType == FileType.DE)  {
            parse_DE(file);
        }
        instance = inst;

    }

    public boolean parse_CHOCO_DR(File file) {
        try {
            BufferedReader buf = new BufferedReader(new FileReader(file));
            String line = buf.readLine();
            String[] numbers;
            n = Integer.parseInt(line);
            dist = new int[n][n];
            dMax = new int[n];
            for (int i = 0; i < n; i++) {
                line = buf.readLine();
                numbers = line.split(" ");
                if (Integer.parseInt(numbers[0]) != i + 1) {
                    throw new UnsupportedOperationException();
                }
                dMax[i] = Integer.parseInt(numbers[1]);
                for (int j = 0; j < n; j++) {
                    dist[i][j] = -1;
                }
            }
            line = buf.readLine();
            int from, to, cost;
            int min = 1000000;
            int max = 0;
            while (line != null) {
                numbers = line.split(" ");
                from = Integer.parseInt(numbers[0]) - 1;
                to = Integer.parseInt(numbers[1]) - 1;
                cost = Integer.parseInt(numbers[2]);
                min = Math.min(min, cost);
                max = Math.max(max, cost);
                if (dist[from][to] != -1) {
                    throw new UnsupportedOperationException();
                }
                dist[from][to] = dist[to][from] = cost;
                line = buf.readLine();
            }
            lb = (n - 1) * min;
            ub = (n - 1) * max;
            //            setUB(dirOpt, s);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        throw new UnsupportedOperationException();
    }


    public boolean parse_ANDINST(File file) {
        try {
            BufferedReader buf = new BufferedReader(new FileReader(file));
            String line = buf.readLine();
            String[] numbers;
            n = Integer.parseInt(line.split(" ")[0]);
            dist = new int[n][n];
            dMax = new int[n];
            int m = Integer.parseInt(line.split(" ")[1].trim());
            line = buf.readLine();
            int from, to, cost;
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int i = 0; i < m; i++) {
                numbers = line.split(" ");
                from = Integer.parseInt(numbers[0]) - 1;
                to = Integer.parseInt(numbers[1]) - 1;
                cost = Integer.parseInt(numbers[2]);
                min = Math.min(min, cost);
                max = Math.max(max, cost);
                dist[from][to] = dist[to][from] = cost;
                line = buf.readLine();
            }

            for (int i = 0; i < n; i++) {
                numbers = line.split(" ");

                dMax[Integer.parseInt(numbers[0]) - 1] = Integer.parseInt(numbers[1]);


                line = buf.readLine();
            }

            lb = (n - 1) * min;
            ub = (n - 1) * max;
            //            setUB(dirOpt, s);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        throw new UnsupportedOperationException();
    }

    public boolean parse_DR_LH(File file) {
        try {
            BufferedReader buf = new BufferedReader(new FileReader(file));
            buf.readLine();
            String line = buf.readLine();
            String[] numbers;
            n = Integer.parseInt(line.split(" ")[0]);
            dist = new int[n][n];
            dMax = new int[n];
            int m = Integer.parseInt(line.split(" ")[1].trim());

            //read degrees bounds
            for (int i = 0; i < n; i++) {
                line = buf.readLine();
                dMax[i] = Integer.parseInt(line.trim());
            }

            int from, to, cost;
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int i = 0; i < m; i++) {
                line = buf.readLine();
                numbers = line.split(" ");
                from = Integer.parseInt(numbers[0]) - 1;
                to = Integer.parseInt(numbers[1]) - 1;
                cost = Integer.parseInt(numbers[2]);
                min = Math.min(min, cost);
                max = Math.max(max, cost);
                dist[from][to] = dist[to][from] = cost;
            }

            lb = (n - 1) * min;
            ub = (n - 1) * max;
            //            setUB(dirOpt, s);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        throw new UnsupportedOperationException();
    }

    public boolean parse_DE(File file) {
        try {
            BufferedReader buf = new BufferedReader(new FileReader(file));
            buf.readLine();
            buf.readLine();
            String line = buf.readLine();
            String[] numbers;
            n = Integer.parseInt(line.split("\\s+")[1].trim());
            dist = new int[n][n];
            dMax = new int[n];
            int m = Integer.parseInt(line.split("\\s+")[2].trim());
            line = buf.readLine();
            int from, to, cost;
            int min = Integer.MAX_VALUE;
            int max = 0;

            for (int i = 0; i < n; i += 5) {
                numbers = line.split("\\s+");
                for (int j = 0; j < 5; j++) {
                    dMax[i+j] = Integer.parseInt(numbers[j+1].trim());
                }
                line = buf.readLine();
            }

            for (int i = 0; i < m; i+= 3) {
                numbers = line.split("\\s+");

                for (int j = 0; j < numbers.length/3; j++) {
                    from = Integer.parseInt(numbers[3*j+1]) - 1;
                    to = Integer.parseInt(numbers[3*j+1+1]) - 1;
                    cost = Integer.parseInt(numbers[3*j+2+1]);
                    min = Math.min(min, cost);
                    max = Math.max(max, cost);
                    dist[from][to] = dist[to][from] = cost;
                }
                line = buf.readLine();

            }

            lb = (n - 1) * min;
            ub = (n - 1) * max;
            //            setUB(dirOpt, s);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        throw new UnsupportedOperationException();
    }


    public enum FileType {
        ANDINST, DR_LH, DE, CHOCO_DR;
    }

}
