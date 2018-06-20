package fsw;

import util.base.Line;
import util.base.LineComparatorIDP;
import util.base.Segments;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PLR_FSW {
    private int[] point;
    private int seg_FSW_num;
    private int seg_SFSW_num;
    private int seg_Back_num;
    private double[] eui;
    private List<Integer> segList;
    private List<Integer> segBackList;
    private List<Integer> segStepList;
    private List<Line> lineList;
    private double data[];
    private double slope;
    private double up_slope;
    private double down_slope;
    private List<Integer> segtemp;

    private Instant fswStartTime;
    private Instant fswEndTime;
    private Instant sfswStartTime;
    private Instant sfswEndTime;

    public long getSfswDuration() {
        Duration duration = Duration.between(sfswStartTime, sfswEndTime);
        return duration.toMillis();
    }

    public long getFswDuration() {
        Duration duration = Duration.between(fswStartTime, fswEndTime);
        return duration.toMillis();
    }

    public PLR_FSW(Double[] oriData) {
        int length = oriData.length;
        seg_FSW_num = 0;
        seg_SFSW_num = 0;
        seg_Back_num = 0;
        point = new int[length];
        eui = new double[length];
        data = new double[length];
        for (int i = 0; i < length; i++) {
            point[i] = 0;// Ĭ��δѡȡ
            eui[i] = Double.MAX_VALUE;
            if (oriData[i] != null) {
                data[i] = oriData[i];
            } else {
                break;
            }
        }
        slope = up_slope = Double.MAX_VALUE;
        down_slope = -Double.MAX_VALUE;
        lineList = new ArrayList<Line>();
        segList = new ArrayList<Integer>();
        segBackList = new ArrayList<Integer>();
        segStepList = new ArrayList<Integer>();
        segtemp = new ArrayList<Integer>();
    }

    public int getSeg_Back_num() {
        return seg_Back_num;
    }

    public int getSeg_FSW_num() {
        return seg_FSW_num;
    }

    public void setSeg_FSW_num(int seg_num) {
        this.seg_FSW_num = seg_num;
    }

    public int getSeg_SFSW_num() {
        return seg_SFSW_num;
    }

    public void setSeg_SFSW_num(int seg_num) {
        this.seg_SFSW_num = seg_num;
    }

    public List<Integer> getSegtemp() {
        return segtemp;
    }

    public void setSegtemp(List<Integer> segtemp) {
        this.segtemp = segtemp;
    }

    /**
     * ����б�ʷֶ�
     *
     * @param threshold �ֶ����
     * @return
     */
    @SuppressWarnings("unchecked")
    public List segmentBySlope(double threshold) {
        fswStartTime = Instant.now();
//        System.out.println(System.currentTimeMillis());
        int current_csp, i;
        int start_pos = current_csp = 0;
        segList.add(start_pos);
        current_csp = start_pos + 1;
        while (current_csp < data.length - 1) {
            i = current_csp + 1;
            up_slope = (data[current_csp] - data[start_pos] + threshold);
            down_slope = (data[current_csp] - data[start_pos] - threshold);
            while (up_slope >= down_slope && i <= data.length - 1) {
                slope = (data[i] - data[start_pos]) / (i - start_pos);
                if (slope <= up_slope && slope >= down_slope) {
                    current_csp = i;
                }
                update_slope(i, start_pos, threshold);
                i++;

            }
            start_pos = current_csp;
            current_csp++;
            segList.add(start_pos);
            seg_FSW_num++;

        }
        if (current_csp == data.length - 1) {
            segList.add(current_csp);
            seg_FSW_num++;
        }
//        System.out.println(System.currentTimeMillis());
        fswEndTime = Instant.now();

        return segList;

    }

    /**
     * ����б�ʲ�ηֶ�
     *
     * @param threshold �ֶ����
     * @return
     */
    @SuppressWarnings("unchecked")
    public List segmentByStepSlope(double threshold) {
        sfswStartTime = Instant.now();

        int current_csp, i, start, mid, end, loc, newpoint;
        int start_pos = current_csp = 0;
        segStepList.add(start_pos);
        segtemp.add(start_pos);
        current_csp = start_pos + 1;
        while (current_csp < data.length - 1) {
            i = current_csp + 1;
            up_slope = (data[current_csp] - data[start_pos] + threshold);
            down_slope = (data[current_csp] - data[start_pos] - threshold);
            /*
             * up_slope = (data[1]+threshold)-data[0]/1; down_slope =
             * (data[1]-threshold)-data[0]/1;
             */
            while (up_slope >= down_slope && i <= data.length - 1) {
                slope = (data[i] - data[start_pos]) / (i - start_pos);
                if (slope <= up_slope && slope >= down_slope) {
                    current_csp = i;
                    // segList.add(current_csp);
                }
                update_slope(i, start_pos, threshold);
                i++;

            }
            /*
             * if(up_slope <= down_slope){ start_pos = current_csp;
             * segList.add(start_pos); seg_num++; }
             */
            start_pos = current_csp;
            current_csp++;
            segtemp.add(start_pos);
            if (segtemp.size() == 3) {
                start = segtemp.get(2);
                mid = segtemp.get(1);
                end = segtemp.get(0);
                segtemp.removeAll(segtemp);
                newpoint = mid;
                loc = segmentBySlopeBack(start, end, threshold);
                double sumError = this.calcError(end, mid) + this.calcError(mid, start);
                while (loc < mid) {
                    double maxError = 0;
                    for (int j = end + 1; j <= start - 1; j++) {
                        if (j <= loc) {
                            maxError += dist(end, data[end], loc, data[loc], j, data[j]);
                        } else {
                            maxError += dist(loc, data[loc], start, data[start], j, data[j]);
                        }

                    }

                    if (maxError < sumError) {
                        sumError = maxError;
                        newpoint = loc;
                    }
                    loc++;
                }
                if (newpoint != mid) {
                    segStepList.remove(Integer.valueOf(mid));
                    seg_SFSW_num--;
                    start_pos = newpoint;
                    current_csp = start_pos + 1;
                }
                segtemp.add(start_pos);
            }
            segStepList.add(start_pos);
            seg_SFSW_num++;
            if (current_csp == data.length - 1) {
                segStepList.add(current_csp);
                seg_SFSW_num++;
            }
        }

        sfswEndTime = Instant.now();

        return segStepList;

    }

    /**
     * ����б�ʷ���ֶ�
     *
     * @param threshold �ֶ����
     * @return
     */
    @SuppressWarnings("unchecked")
    public int segmentBySlopeBack(int start, int end, double threshold) {
        int current_csp, i;
        int start_pos = current_csp = start;
        current_csp = start_pos - 1;
        i = current_csp - 1;
        up_slope = (data[current_csp] - data[start_pos] + threshold);
        down_slope = (data[current_csp] - data[start_pos] - threshold);
        while (up_slope >= down_slope && start - i <= start - end) {
            slope = (data[i] - data[start_pos]) / (start_pos - i);
            if (slope <= up_slope && slope >= down_slope) {
                current_csp = i;
                //return current_csp;
            }
            update_stepslope(i, start_pos, threshold);
            i--;
        }
        return current_csp;
    }

    public List segmentBySlopeBackfull(int start, int end, double threshold) {
        int current_csp, i;
        int start_pos = current_csp = start;
        segBackList.add(start_pos);
        current_csp = start_pos - 1;
        while (start - current_csp < start - end) {
            up_slope = (data[current_csp] - data[start_pos] + threshold);
            down_slope = (data[current_csp] - data[start_pos] - threshold);
            i = current_csp - 1;
            while (up_slope >= down_slope && start - i <= start - end) {
                slope = (data[i] - data[start_pos]) / (start_pos - i);
                if (slope <= up_slope && slope >= down_slope) {
                    current_csp = i;
                    //return current_csp;
                }
                update_stepslope(i, start_pos, threshold);
                i--;
            }
            start_pos = current_csp;
            current_csp--;
            segBackList.add(start_pos);
            seg_Back_num++;
        }
        if (current_csp == 0) {
            segBackList.add(current_csp);
            seg_Back_num++;
        }

        return segBackList;

    }

    private void update_slope(int cur, int start, double threshold) {
        // TODO Auto-generated method stub
        double temp1 = (data[cur] - data[start] + threshold) / (cur - start);
        double temp2 = (data[cur] - data[start] - threshold) / (cur - start);
        up_slope = Math.min(up_slope, temp1);
        down_slope = Math.max(down_slope, temp2);
    }

    private void update_stepslope(int cur, int start, double threshold) {
        // TODO Auto-generated method stub
        double temp1 = (data[cur] - data[start] + threshold) / (start - cur);
        double temp2 = (data[cur] - data[start] - threshold) / (start - cur);
        up_slope = Math.min(up_slope, temp1);
        down_slope = Math.max(down_slope, temp2);
    }

    // �������
    public double calcError(List seg) {
        double sum_Err = 0.0;
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.000000");
        //return Math.abs(Double.parseDouble(df.format((x0 - x1)* (y2 - y1) / (x2 - x1))) + Double.parseDouble(df.format(y1 - y0)));
        for (int i = 0; i < seg.size(); i++) {
            int begin = (Integer) seg.get(i);
            if (i + 1 < seg.size()) {
                int end = (Integer) seg.get(i + 1);
                for (int j = begin + 1; j < end; j++) {
                    sum_Err += dist(begin, data[begin], end, data[end], j,
                            data[j]);
                    sum_Err = Double.parseDouble(df.format(sum_Err));
                    //System.out.println("begin: "+begin+" end: "+ end+" sum_error: "+sum_Err);
                }
            }

        }
        return sum_Err;
    }

    // �������
    public double calcError(int[] seg) {
        double sum_Err = 0.0;
        for (int i = 0; i < seg.length; i++) {
            int begin = seg[i];
            if (i + 1 < seg.length) {
                int end = seg[i + 1];
                for (int j = begin + 1; j < end; j++) {
                    sum_Err += dist(begin, data[begin], end, data[end], j, data[j]);
                }
            }

        }
        return sum_Err;
    }

    /**
     * ����ֵ�ֶ�
     *
     * @param threshold �ֶ����
     * @return
     */
    @SuppressWarnings("unchecked")
    public int[] choosePointIDPByThreshold(double threshold) {
        point[0] = 1;
        point[data.length - 1] = 1;
        updataInfo(0, data.length - 1);
        Line line;
        do {
            line = lineList.get(0);
            point[line.getPmax()] = 1;
            updataInfo(line.getBegin(), line.getPmax());
            updataInfo(line.getPmax(), line.getEnd());
            lineList.remove(0);
            lineList.sort(new LineComparatorIDP());
            line = lineList.get(0);
        } while (line.getWeight() >= threshold);
        return point;

    }

    /**
     * ������
     *
     * @param number �ֶθ���
     * @return
     */
    public int[] choosePointIDPByNumber(int number) {
        point[0] = 1;
        point[data.length - 1] = 1;
        updataInfo(0, data.length - 1);
        lineList.sort(new LineComparatorIDP());
        int pointNumber = 2;
        while (pointNumber < number) {
            pointNumber++;
            Line line = lineList.get(0);
            point[line.getPmax()] = 1;
            updataInfo(line.getBegin(), line.getPmax());
            updataInfo(line.getPmax(), line.getEnd());
            lineList.remove(0);
            lineList.sort(new LineComparatorIDP());
        }
        return point;

    }

    // ������Ϣ
    public void updataInfo(int begin, int end) {
        int pmax = begin;
        double dist = 0;
        double distmax = 0;
        eui[begin] = 0;
        eui[end] = 0;
        for (int i = begin + 1; i < end; i++) {
            eui[i] = dist(begin + 1, data[begin], end + 1, data[end], i + 1,
                    data[i]);
            dist += eui[i];
            if (eui[i] > distmax) {
                pmax = i;
                distmax = eui[i];
            }
        }
        // weight����PLR_IDP��PLR_SIP��ͬ
        double weight = 2 * distmax > dist ? 2 * distmax : dist;
        Line line = new Line(begin, end, dist, distmax, pmax, weight);
        lineList.add(line);


    }

    // ����㵽ֱ�ߵľ���
    public double dist(int x1, double y1, int x2, double y2, int x0,
                       double y0) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.0000");
        return Math.abs(Double.parseDouble(df.format((x0 - x1) * (y2 - y1) / (x2 - x1))) + Double.parseDouble(df.format(y1 - y0)));
    }

    // ����㵽ֱ�ߵľ���
    public double dist(double x1, double y1, double x2, double y2, double x0,
                       double y0) {
        return Math.abs((x0 - x1) * (y2 - y1) / (x2 - x1) + y1 - y0);
    }

    // ����ֵ��������
    public int[] getIDPIndexByThreshold(double threshold) {
        int[] IDP = choosePointIDPByThreshold(threshold);
        List<Integer> list = new ArrayList<Integer>();
        int number = 0;
        for (int i = 0; i < IDP.length; i++) {
            if (IDP[i] == 1) {
                number++;
                list.add(i);
            }
        }
        int[] IDPindex = new int[number];
        for (int i = 0; i < list.size(); i++) {
            IDPindex[i] = list.get(i);
        }
        return IDPindex;
    }

    // ��������������
    public int[] getIDPIndexByNumber(int number) {
        int[] IDP = choosePointIDPByNumber(number);

        int[] IDPindex = new int[number];
        int index = 0;
        for (int i = 0; i < IDP.length; i++) {
            if (IDP[i] == 1) {
                IDPindex[index] = i;
                index++;
            }
        }
        return IDPindex;
    }

    public double calcError(int begin, int end) {
        double sum_Err = 0.0;
        for (int i = begin + 1; i < end; i++) {
            sum_Err += dist(begin, data[begin], end, data[end], i, data[i]);
        }
        return sum_Err;
    }

    public List<Segments> composeFSWSegments(List fswSegs) {
        if (fswSegs == null || fswSegs.isEmpty())
            return new ArrayList<Segments>();
        List<Segments> results = new ArrayList<Segments>();
        for (int i = 0; i < fswSegs.size() - 1; i++) {
            int begin = (Integer) fswSegs.get(i);
            int end = (Integer) fswSegs.get(i + 1);
            double segErr = this.calcError(begin, end);
            Segments seg = new Segments(begin, end, segErr, segErr);
            results.add(seg);
        }
        return results;
    }

    public double[] getUpdateTSBySegPoints(List<Integer> points) {
        double updateData[] = new double[data.length];
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.0000");
        int begin = 0;
        int end = 0;

        updateData[0] = data[0];
        while (points.size() > 1) {
            begin = points.get(0);
            end = points.get(1);
            updateData[end] = data[end];
            for (int i = begin + 1; i < end; i++) {
                updateData[i] = (updateData[end] - updateData[begin])
                        / (end - begin) * (i - begin) + updateData[begin];
                updateData[i] = Double.parseDouble(df.format(updateData[i]));
            }
            points.remove(0);
        }

        return updateData;
    }

}
