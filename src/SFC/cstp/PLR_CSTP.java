package cstp;


import util.base.Line;
import util.base.LineComparatorIDP;
import util.base.Segments;
import util.DlTools;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PLR_CSTP {
    private int[] point;
    private int seg_FSW_num;
    private int seg_SFSW_num;
    private int seg_Back_num;
    private int seg_CSTP_num;
    private double[] eui;
    private List<Integer> segList;
    private List<Integer> segBackList;
    private List<Integer> segStepList;
    private List<Integer> segTP1List;
    private List<Integer> segTP2List;
    private List<Integer> segCSTPList;
    private List<Segments> segRefineList;
    private List<Line> lineList;
    private double data[];
    private double slope;
    private double up_slope;
    private double down_slope;
    private List<Integer> segtemp;
    private double max_error = 1.0;
    private double point_max_error = 1.0;

    private Instant startTime;
    private Instant endTime;

    public long getCSTPDuration() {
        Duration duration = Duration.between(startTime, endTime);
        return duration.toMillis();
    }

    public PLR_CSTP(Double[] oriData) {
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
        segTP1List = new ArrayList<Integer>();
        segTP2List = new ArrayList<Integer>();
        segCSTPList = new ArrayList<Integer>();
        segRefineList = new ArrayList<Segments>();
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

    public int getSeg_CSTP_num() {
        return seg_CSTP_num;
    }

    public void setSeg_CSTP_num(int seg_CSTP_num) {
        this.seg_CSTP_num = seg_CSTP_num;
    }

    public List<Integer> getSegtemp() {
        return segtemp;
    }

    public void setSegtemp(List<Integer> segtemp) {
        this.segtemp = segtemp;
    }

    // �������By range
    public int calcError(int begin, int end, double threshold) {
        double sum_Err = 0.0;
            /*if(end >= data.length){
                return -1;
			}*/
        for (int i = begin + 1; i < end; i++) {
            sum_Err += dist(begin, data[begin], end, data[end], i, data[i]);
            if (sum_Err > threshold) {
                return -1; // 此segment累积超过阈值
            }
        }
        return 1;
    }

    /**
     * ����б��&ת�۵�ֶ�
     *
     * @param �ֶ����
     * @return
     */
    @SuppressWarnings("unchecked")
    public List segmentBySLTP(double thresholdpoint, double thresholdseg) {
        max_error = thresholdseg;
        point_max_error = thresholdpoint;
        startTime = Instant.now();

        segCSTPList.clear();
        seg_CSTP_num = 0;
        List<Integer> segFirstList = segmentBySL(thresholdpoint);
        /*System.out.println("TP2: ");
		System.out.print(segTP2List);
		System.out.println();
		System.out.println("TP1: ");
		System.out.print(segTP1List);*/
        segmentRefine(segFirstList, thresholdseg);
        Segments segcheck = segRefineList.get(0);
        int len = segcheck.getBegin();
        int end = segRefineList.get(segRefineList.size() - 1).getBegin();
        int loc = 0;
        while (len <= end) {//打散标记11的segment
            segcheck = segRefineList.get(loc);
            len = segcheck.getEnd();
            if (segcheck.getIsFirst() == 11) {
                segRefineList.remove(loc);
                for (int i = segcheck.getBegin(); i < segcheck.getEnd(); i++) {
                    Segments segnew = new Segments(i, i + 1, 0);
                    segRefineList.add(loc, segnew);
                    loc = loc + 1;
                }
            } else {
                loc = loc + 1;
            }
        }
		/*System.out.println("First time");
		for (Segments segment : segRefineList) {
			if(segment.getBegin()+1!=segment.getEnd()){
				System.out.println(segment.getBegin()+"--->"+segment.getEnd());
			}
		}
		System.out.println("End First time");*/
        segCSTPList.add(segRefineList.get(0).getBegin());
        segCSTPList.add(segRefineList.get(0).getEnd());
        seg_CSTP_num++;
        segRefineList.remove(0);
        try {
            segcheck = segRefineList.get(0);
            while (segcheck.getIsFirst() == 1) {
                segCSTPList.add(segcheck.getEnd());
                seg_CSTP_num++;
                segRefineList.remove(segcheck);
                if (segRefineList.size() == 0) {
                    endTime = Instant.now();
                    return segCSTPList;
                } else {
                    segcheck = segRefineList.get(0);
                }
            }
            Segments minsegment = segPreSWAB();
            segSWAB(minsegment, thresholdseg);
        } catch (Exception e) {
            System.out.println("唯一分段");
        }


        endTime = Instant.now();

        return segCSTPList;
    }

    private void segSWAB(Segments minsegment, double thresholdseg) {
        double cost = 0.0;
        if (minsegment != null) {
            int minloc = minsegment.getIndexloc();
            double mercost = minsegment.getMegerCost();
			/*System.out.println("segswab");
			for (Segments segment : segRefineList) {

					System.out.println(segment.getBegin()+"--->"+segment.getEnd());
			}*/
            //System.out.println("segswab");
            if (minloc >= 0 && minloc < segRefineList.size()) {
                while (mercost <= thresholdseg) {
                    if (segRefineList.size() == 62) {
                        //System.out.println();
                    }
                    //System.out.println(minloc);
                    int temploc = 0;
                    Segments newseg = segRefineList.get(minloc);
//						System.out.println("checked the begin: "+ newseg.getBegin());
                    newseg.setEnd(segRefineList.get(minloc + 1).getEnd());
                    if (minloc + 2 < segRefineList.size() && (segRefineList.get(minloc + 2).getIsFirst() != 1)) {
                        cost = mergeNext(newseg, segRefineList.get(minloc + 2));
                        newseg.setMegerCost(cost);
                    } else if (minloc + 2 < segRefineList.size() && segRefineList.get(minloc + 2).getIsFirst() == 1) {
                        cost = Double.MAX_VALUE;
                        newseg.setMegerCost(cost);
                    }
                    if (minloc - 1 >= 0) {
                        Segments preseg = segRefineList.get(minloc - 1);
                        if (preseg.getIsFirst() != 1) {
                            cost = mergeNext(preseg, newseg);
                            preseg.setMegerCost(cost);
                            segRefineList.set(minloc - 1, preseg);
                        }
                    }
                    segRefineList.set(minloc, newseg);
                    segRefineList.remove(minloc + 1);
                    mercost = Double.MAX_VALUE;
                    while (temploc < segRefineList.size() - 1) {
                        if (mercost > segRefineList.get(temploc).getMegerCost()) {
                            mercost = segRefineList.get(temploc).getMegerCost();
                            minloc = temploc;
                        }
                        temploc++;
                    }
                }
                //System.out.println();
                for (Segments segment : segRefineList) {
                    segCSTPList.add(segment.getEnd());
                    seg_CSTP_num++;
//					System.out.println("--"+segment.getEnd()+"--");
                }
                //System.out.println();
            }
        } else {
            //System.out.println("PreSWAB is error!");
        }
    }

    private Segments segPreSWAB() {
        int len = 0;
        double cost = 0.0;
        double mincost = Double.MAX_VALUE;
        Segments minsegment = null;
        while (len + 1 < segRefineList.size()) {
            Segments segcur = segRefineList.get(len);
            Segments segnext = segRefineList.get(len + 1);
            segcur.setIndexloc(len);
            segnext.setIndexloc(len + 1);
            if (segcur.getIsFirst() == 1 || segnext.getIsFirst() == 1) {
                segcur.setMegerCost(Double.MAX_VALUE);
                segnext.setMegerCost(Double.MAX_VALUE);
                segRefineList.set(len, segcur);
                segRefineList.set(len + 1, segnext);
                len = len + 2;
            } else {
                cost = mergeNext(segcur, segnext);
                //System.out.println(cost);
                if (cost < mincost) {
                    mincost = cost;
                    minsegment = segcur;
                }
                segcur.setMegerCost(cost);
                segRefineList.set(len, segcur);
                //segRefineList.set(len+1, segnext);
                len = len + 1;
            }
        }
        Segments lastsegment = segRefineList.get(segRefineList.size() - 1);
        lastsegment.setMegerCost(Double.MAX_VALUE);
        segRefineList.set(segRefineList.size() - 1, lastsegment);
		/*for (Segments segment : segRefineList) {
			System.out.println(segment.getBegin()+"--->"+segment.getEnd()+" loc: "+segment.getIndexloc()+" mercost: "+segment.getMegerCost());
		}
		System.out.println("SEcond time");
		for (Segments segment : segRefineList) {
			if(segment.getBegin()+1!=segment.getEnd()){
				System.out.println(segment.getBegin()+"--->"+segment.getEnd());
			}
		}*/
        return minsegment;
    }

    public double mergeNext(Segments segmin, Segments next) {
        int begin = segmin.getBegin();
        int end = next.getEnd();
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.000000");
        double summercost = 0.0;
        for (int i = begin + 1; i < end; i++) {
            summercost += dist(begin, data[begin], end, data[end], i, data[i]);
            summercost = Double.parseDouble(df.format(summercost));
        }
        return summercost;
    }

    public void segmentRefine(List<Integer> segFirstList, double thresholdseg) {
        //List<Segments> segReTemp = new ArrayList<Segments>();
        int loc = segFirstList.get(0);
        int nextloc = loc;
        int i = 1;
        int temp = 2;
        while (i < segFirstList.size()) {
            nextloc = segFirstList.get(i);
            temp = calcError(loc, nextloc, thresholdseg);
            if (temp == 1) {
                Segments newseg = new Segments(loc, nextloc);  //full fsw segment
                segRefineList.add(newseg);
            } else {
                segmentReTP2(loc, nextloc, thresholdseg);
            }
            loc = nextloc;
            i++;
        }
    }

    private void segmentReTP2(int loc, int nextloc, double thresholdseg) {
        int begin = 0;
        int end = 0;
        int temp = 0;
        int beginloc = loc;
        for (int i : segTP2List) {
            if (i > loc) {
                begin = i;
                //if(i > loc){ begin = segTP2List.get(i-1); break;}
                break;
            }
        }
        for (int i : segTP2List) {
            if (i >= nextloc) {
                try {
                    end = segTP2List.get(segTP2List.indexOf(i) - 1);
                } catch (Exception e) {
//                    e.printStackTrace();
                }
                break;
            }
        }

        if (begin < end) {
            beginloc = begin;
            while (begin <= end) {
                temp = calcError(loc, begin, thresholdseg);
                if (temp != 1) {
                    if (beginloc == loc) {
                        segmentReTP1(loc, begin, thresholdseg);
                        loc = beginloc = begin;
                    } else {
                        Segments newseg = new Segments(loc, beginloc, 0);
                        segRefineList.add(newseg);
                        if (calcError(beginloc, begin, thresholdseg) != 1) {
                            segmentReTP1(beginloc, begin, thresholdseg);
                            loc = beginloc = begin;
                        } else {
                            loc = beginloc;
                        }
                    }
                } else {
                    beginloc = begin;
                }
                begin = segTP2List.get(segTP2List.indexOf(begin) + 1);
            }
            if (calcError(loc, nextloc, thresholdseg) == 1) {
                Segments newseg = new Segments(loc, nextloc, 0);
                segRefineList.add(newseg);
            } else {
                if (loc != beginloc) {
                    Segments newseg1 = new Segments(loc, beginloc, 0);
                    segRefineList.add(newseg1);
                    segmentReTP1(beginloc, nextloc, thresholdseg);
                } else {
                    segmentReTP1(loc, nextloc, thresholdseg);
                }
            }
        } else {
            segmentReTP1(loc, nextloc, thresholdseg);
        }
    }

    private void segmentReTP1(int loc, int nextloc, double thresholdseg) {
        int begin = 0;
        int end = 0;
        int temp = 0;
        int beginloc = 0;
        int firstloc = loc;
        for (int i : segTP1List) {
            if (i >= loc) {
                begin = i;
                break;
            }
        }
        for (int i : segTP1List) {
            if (i >= nextloc) {
                try {
                    end = segTP1List.get(segTP1List.indexOf(i) - 1);
                } catch (Exception e) {
//                    e.printStackTrace();
                }
                break;
            }
        }

        if (begin < end) {
            beginloc = loc;
            while (begin <= end) {
                temp = calcError(loc, begin, thresholdseg);
                if (temp != 1) {
                    if (loc == beginloc) {
                        Segments newseg = new Segments(loc, begin, 11);
                        segRefineList.add(newseg);
                        loc = beginloc = begin;
                    } else {
                        Segments newseg = new Segments(loc, beginloc, 0);
                        segRefineList.add(newseg);
                        if (calcError(beginloc, begin, thresholdseg) != 1) {
                            newseg = new Segments(beginloc, begin, 11);
                            segRefineList.add(newseg);
                            loc = beginloc = begin;
                        } else {
                            loc = beginloc;
                        }
                    }
                } else {
                    beginloc = begin;
                }
                begin = segTP1List.get(segTP1List.indexOf(begin) + 1);
            }
            if (calcError(loc, nextloc, thresholdseg) == 1) {
                Segments newseg = new Segments(loc, nextloc, 0);
                segRefineList.add(newseg);
            } else {
                if (loc != beginloc) {
                    Segments newseg1 = new Segments(loc, beginloc, 0);
                    segRefineList.add(newseg1);
                    Segments newseg2 = new Segments(beginloc, nextloc, 11);
                    segRefineList.add(newseg2);
                } else {
                    Segments newseg = new Segments(loc, nextloc, 11);
                    segRefineList.add(newseg);
                }
            }
        } else {
            Segments newseg = new Segments(loc, nextloc, 11);
            segRefineList.add(newseg);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer> segmentBySL(double threshold) {
        int current_csp, i, temp, tp1;
        int start_pos = current_csp = temp = 0;
        segList.clear();
        seg_FSW_num = 0;
        segList.add(start_pos);
        tp1 = start_pos;
        current_csp = start_pos + 1;
        while (current_csp < data.length - 1) {
            i = current_csp + 1;
            up_slope = (data[current_csp] - data[start_pos] + threshold);
            down_slope = (data[current_csp] - data[start_pos] - threshold);
            while (up_slope >= down_slope && i <= data.length - 1) {
                temp = findTP1TP2(i, tp1);
                if (temp != -1) {
                    tp1 = temp;
                }
                slope = (data[i] - data[start_pos]) / (i - start_pos);
                if (slope <= up_slope && slope >= down_slope) {
                    current_csp = i;
                    // segList.add(current_csp);
                }
                update_slope(i, start_pos, threshold);
                i++;

            }
            start_pos = current_csp;
            current_csp++;
            segList.add(start_pos);
            seg_FSW_num++;

        }
		/*System.out.println("TP2: ");
		System.out.print(segTP2List);
		System.out.println();
		System.out.println("TP1: ");
		System.out.print(segTP1List);*/
        if (current_csp == data.length - 1) {
            segList.add(current_csp);
            seg_FSW_num++;
        }

        return segList;
    }

    public int findTP1TP2(int nextloc, int curloc) {
        if (nextloc > curloc) {
            if (curloc + 1 < data.length - 1) {
                curloc++;
                if ((data[curloc] < data[curloc - 1] && data[curloc] < data[curloc + 1])
                        || (data[curloc] > data[curloc - 1] && data[curloc] > data[curloc + 1])
                        || (data[curloc] <= data[curloc - 1] && data[curloc] == data[curloc + 1])
                        || (data[curloc] == data[curloc - 1] && data[curloc] <= data[curloc + 1])
                        || (data[curloc] == data[curloc - 1] && data[curloc] >= data[curloc + 1])
                        || (data[curloc] >= data[curloc - 1] && data[curloc] == data[curloc + 1])) {
                    segTP1List.add(curloc);
                    if (segTP2List.isEmpty()) {
                        segTP2List.add(curloc);
                    } else {
                        int k = segTP2List.get(segTP2List.size() - 1);
                        double a = data[curloc];
                        double b = data[k];
                        if ((Math.abs(k - curloc) >= 3)
                                && (Math.abs(a - b)) / ((Math.abs(a + b) / 2)) >= 0.5) {
                            //System.out.println("a's loc: "+curloc+" a's value: "+a+" k's loc: "+k+" k's value: "+ b);
                            segTP2List.add(curloc);
                        }
                    }
                }
                return curloc;
            }
        }
        return -1;
    }

    /**
     * ����б�ʷֶ�
     *
     * @param threshold �ֶ����
     * @return
     */
    @SuppressWarnings("unchecked")
    public List segmentBySlope(double threshold) {
        return segmentBySL(threshold);

    }

    /**
     * ����б�ʲ�ηֶ�
     *
     * @param threshold �ֶ����
     * @return
     */
    @SuppressWarnings("unchecked")
    public List segmentByStepSlope(double threshold) {
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
            while (up_slope > down_slope && i <= data.length - 1) {
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
                double sumError = Double.MAX_VALUE;
                while (loc < mid) {
                    double maxError = 0;
                    for (int j = end + 1; j <= start - 1; j++) {
                        if (j <= loc) {
                            maxError += dist(end, data[end], loc, data[loc], j,
                                    data[j]);
                        } else {
                            maxError += dist(loc, data[loc], start,
                                    data[start], j, data[j]);
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
                // return current_csp;
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
                    // return current_csp;
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

    public double calcError(int begin, int end) {
        double sum_Err = 0.0;
        for (int i = begin + 1; i < end; i++) {
            sum_Err += dist(begin, data[begin], end, data[end], i, data[i]);
        }
        return sum_Err;
    }

    // 计算错误倿
    public double calTotalError(List<Segments> list) {
        double sum_Err = 0.0;
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.000000");
        for (int i = 0; i < list.size(); i++) {
            Segments segment = list.get(i);
            double err = calcError(segment.getBegin(), segment.getEnd());
            System.out.println(segment.getBegin() + "--" + segment.getEnd() + ":" + err + "->" + (err < max_error));
            sum_Err += err;
        }
        sum_Err = Double.parseDouble(df.format(sum_Err));
        return sum_Err;
    }

    // 计算错误值
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
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.000000");
        for (int i = 0; i < seg.length; i++) {
            int begin = seg[i];
            if (i + 1 < seg.length) {
                int end = seg[i + 1];
                for (int j = begin + 1; j < end; j++) {
                    sum_Err += dist(begin, data[begin], end, data[end], j,
                            data[j]);
                    sum_Err = Double.parseDouble(df.format(sum_Err));
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
    public double dist(int x1, double y1, int x2, double y2, int x0, double y0) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.0000");
        return Math.abs(Double.parseDouble(df.format((x0 - x1) * (y2 - y1) / (x2 - x1))) + Double.parseDouble(df.format(y1 - y0)));
        //return Math.abs((x0 - x1) * (y2 - y1) / (x2 - x1) + y1 - y0);
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
