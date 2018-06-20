package util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DlTools {
    private static DlTools instance;
    private static List<String> arffFiles = new ArrayList<String>();
    private static List<String> txtFiles = new ArrayList<String>();
    private static List<String> mergeFiles = new ArrayList<String>();
    private static String dataDir = "/home/schong/schong/datas/";
    private static String resultDir = dataDir + "results/";
    java.text.DecimalFormat df = new java.text.DecimalFormat("#.########");

    static {
        /*arffFiles.add(dataDir + "DistalPhalanxOutlineCorrect/DistalPhalanxOutlineCorrect_TEST.arff");
        arffFiles.add(dataDir + "DistalPhalanxOutlineCorrect/DistalPhalanxOutlineCorrect_TRAIN.arff");
        txtFiles.add(dataDir + "DistalPhalanxOutlineCorrect/DistalPhalanxOutlineCorrect_TEST.txt");
        txtFiles.add(dataDir + "DistalPhalanxOutlineCorrect/DistalPhalanxOutlineCorrect_TRAIN.txt");
        mergeFiles.add("DistalPhalanxOutlineCorrect_TEST");
        mergeFiles.add("DistalPhalanxOutlineCorrect_TRAIN");*/

        arffFiles.add(dataDir + "DistalPhalanxTW/DistalPhalanxTW_TEST.arff");
        arffFiles.add(dataDir + "DistalPhalanxTW/DistalPhalanxTW_TRAIN.arff");
        /*txtFiles.add(dataDir + "DistalPhalanxTW/DistalPhalanxTW_TEST.txt");
        txtFiles.add(dataDir + "DistalPhalanxTW/DistalPhalanxTW_TRAIN.txt");
        mergeFiles.add("DistalPhalanxTW_TEST");
        mergeFiles.add("DistalPhalanxTW_TRAIN");*/

        /*arffFiles.add(dataDir + "ECG5000/ECG5000_TEST.arff");
        arffFiles.add(dataDir + "ECG5000/ECG5000_TRAIN.arff");
        txtFiles.add(dataDir + "ECG5000/ECG5000_TEST.txt");
        txtFiles.add(dataDir + "ECG5000/ECG5000_TRAIN.txt");
        mergeFiles.add("ECG5000_TEST");
        mergeFiles.add("ECG5000_TRAIN");

        arffFiles.add(dataDir + "FaceAll/FaceAll_TEST.arff");
        arffFiles.add(dataDir + "FaceAll/FaceAll_TRAIN.arff");
        txtFiles.add(dataDir + "FaceAll/FaceAll_TEST.txt");
        txtFiles.add(dataDir + "FaceAll/FaceAll_TRAIN.txt");
        mergeFiles.add("FaceAll_TEST");
        mergeFiles.add("FaceAll_TRAIN");

        arffFiles.add(dataDir + "HandOutlines/HandOutlines_TEST.arff");
        arffFiles.add(dataDir + "HandOutlines/HandOutlines_TRAIN.arff");
        txtFiles.add(dataDir + "HandOutlines/HandOutlines_TEST.txt");
        txtFiles.add(dataDir + "HandOutlines/HandOutlines_TRAIN.txt");
        mergeFiles.add("HandOutlines_TEST");
        mergeFiles.add("HandOutlines_TRAIN");

        arffFiles.add(dataDir + "StarLightCurves/StarlightCurves_TEST.arff");
        arffFiles.add(dataDir + "StarLightCurves/StarlightCurves_TRAIN.arff");
        txtFiles.add(dataDir + "StarLightCurves/StarlightCurves_TEST.txt");
        txtFiles.add(dataDir + "StarLightCurves/StarlightCurves_TRAIN.txt");
        mergeFiles.add("StarlightCurves_TEST");
        mergeFiles.add("StarlightCurves_TRAIN");

        arffFiles.add(dataDir + "Yoga/Yoga_TEST.arff");
        arffFiles.add(dataDir + "Yoga/Yoga_TRAIN.arff");
        txtFiles.add(dataDir + "Yoga/Yoga_TEST.txt");
        txtFiles.add(dataDir + "Yoga/Yoga_TRAIN.txt");
        mergeFiles.add("Yoga_TEST");
        mergeFiles.add("Yoga_TRAIN");*/

    }

    private DlTools() {
    }

    public static synchronized DlTools getInstance() {
        if (instance == null) {
            instance = new DlTools();
        }
        return instance;
    }

    public List<String> getArffFiles() {
        return arffFiles;
    }

    public List<String> getTxtFiles() {
        return txtFiles;
    }

    public String getResultDir() {
        return resultDir;
    }

    public List<String> getMergeFiles() {
        return mergeFiles;
    }

    public void deleteResultFiles() {
        File dir = FileUtil.file(resultDir);
        File[] filesInDir = dir.listFiles();
        if (filesInDir == null || filesInDir.length == 0) {
            System.out.println("No need to clean!");
            return;
        }
        for (File f : filesInDir) {
            System.out.println("Deleting File : " + f.getName());
            f.delete();
        }
    }

    public List<double[]> readTxt(File file) {
        List<double[]> datas = new ArrayList<>();
        FileReader reader = new FileReader(file);
        List<String> lines = reader.readLines();
        for (String line : lines) {
            String[] lineData = line.split(",");
            double[] doubleLineData = new double[lineData.length - 1];
            for (int i = 1; i < lineData.length; i++) {
                doubleLineData[i - 1] = Double.parseDouble(lineData[i]);
            }
            datas.add(doubleLineData);
        }
        return datas;
    }

    public List<Double[]> readTxtDouble(File file) {
        List<Double[]> datas = new ArrayList<>();
        FileReader reader = new FileReader(file);
        List<String> lines = reader.readLines();
        for (String line : lines) {
            String[] lineData = line.split(",");
            Double[] doubleLineData = new Double[lineData.length - 1];
            for (int i = 1; i < lineData.length; i++) {
                doubleLineData[i - 1] = Double.parseDouble(lineData[i]);
            }
            datas.add(doubleLineData);
        }
        return datas;
    }

    public void stringToFile(String fileName, String data) {
        FileWriter writer = new FileWriter(resultDir + fileName);
        writer.write(data);
    }

    /**
     * Maximum Error Percentage of Single Point
     *
     * @param datas
     * @param percent
     * @return
     */
    public double calculateMEPSP(Double[] datas, double percent) {
        if (datas == null || datas.length == 0)
            return -1;
        List<Double> dataList = CollectionUtil.newArrayList(datas);
        Collections.sort(dataList);
        double min = dataList.get(0);
        double max = dataList.get(dataList.size() - 1);
        return (max - min) * percent / 100;
    }

    public double calculateMEPES(double mepsp, int times) {
        return mepsp * times;
    }

    /**
     * @param fileName
     * @return 0test  1train
     */
    public int testOrTrain(String fileName) {
        return fileName.indexOf("TEST") > 0 ? 1 : 2;
    }

    public Double[] addNewData(Double[] a, double val) {
        Double[] b = new Double[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i].doubleValue() + val;
        }
        return b;
    }

    public void makeArff(File file) {
        FileReader reader = FileReader.create(file);
        FileWriter writer = FileWriter.create(FileUtil.file(file.getName() + "_tmp.arff"));
        List<String> oriLines = reader.readLines();
        int label = -1;
        for (int i = 0; i < oriLines.size(); i++) {
            String line = oriLines.get(i);
            if (line.equals("@data")) {
                label++;
            } else if (label == 0) {
                label++;
            }
            if (label > 0) {
                int testOrTrain = this.testOrTrain(file.getName());

                String[] strs = line.split(",");
                Double[] datas = new Double[strs.length - 1];
                for (int z = 0; z < strs.length - 1; z++) {
                    datas[z] = Double.valueOf(strs[z]);
                }
                if (testOrTrain == 2) {
                    for (int n = 1; n <= 20; n++) {
                        StringBuffer sb = new StringBuffer();
                        for (int m = 0; m < datas.length; m++) {
//                            double val = datas[m] + n;
                            double val = datas[m];
                            sb.append(df.format(val));
                            sb.append(",");
                        }
                        sb.append(strs[strs.length - 1]);
                        writer.append(sb.toString() + "\n");
                    }
                } else if (testOrTrain == 1) {
//                    for (int n = 0; n < 2; n++) {
                    StringBuffer sb = new StringBuffer();
                    for (int m = 0; m < datas.length; m++) {
//                            double val = datas[m] + n * 10;
//                            double val = datas[m] + 20;
                        double val = datas[m];
                        sb.append(df.format(val));
                        sb.append(",");
                    }
                    sb.append(strs[strs.length - 1]);
                    writer.append(sb.toString() + "\n");
//                    }
                } else {

                }
            } else {
                writer.append(line + "\n");
            }
        }
    }
}
