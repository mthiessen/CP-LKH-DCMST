package org.ucc.insight.DCMST;


import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class DCMSTTest {


    public static void all(String subpath) {
        String path = "res/dcmst/allinstances/"+subpath;
        File dir = new File(path);
        File[] directoryListing = dir.listFiles();
        
        if (directoryListing != null) {
            for (File inst : directoryListing) {
                System.out.println("====================================================");
                System.out.println(inst.getName());
                System.out.println("====================================================");

                DCMSTFileReader.FileType ft = DCMSTFileReader.FileType.ANDINST;
                if (subpath.equals("DE")) {
                    ft = DCMSTFileReader.FileType.DE;
                } else if (subpath.equals("LH/Euc")) {
                    ft = DCMSTFileReader.FileType.DR_LH;
                }

                DCMSTFileReader reader = new DCMSTFileReader(path,inst.getName(), ft);
                DCMSTWrapper dcmst = reader.generateDCMSTWrapper();
                dcmst.setOutputType(DCMSTWrapper.FOUND_SOLUTIONS);

               System.out.println("========================");
                System.out.println("=====B&B=========");
                System.out.println("========================");
                dcmst.solveBranchAndBound();

                System.out.println("========================");
                System.out.println("=====B&B with LKH=========");
                System.out.println("========================");
                dcmst.solveBranchAndBound(true, 1);

               System.out.println("========================");
                System.out.println("====Bottom up==========");
                System.out.println("========================");
                dcmst.solveBottomUp();

                System.out.println("================================================");


            }
        }
    }


    
    public static void main(String[] args) {
        all("DE");
    }
}
