package bolt;

import cstp.PLR_CSTP;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import util.DlTools;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CSTPBolt extends BaseRichBolt {
    private OutputCollector _collector;
    private static ConcurrentHashMap<Integer,double[]> rCSTPData = new ConcurrentHashMap<>();
    public ConcurrentHashMap<Integer,double[]> getrCSTPData(){
        return rCSTPData;
    }
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this._collector=collector;
    }

    private static Instant endTime;

    public void execute(Tuple tuple) {

        double[] data = (double[])tuple.getValue(0);
        int index = tuple.getInteger(1);
        int interval = data.length-1;
        Double[] temp = new Double[interval];
        for(int i=0; i<interval; i++){
            temp[i]=data[i];
        }
        //start
        PLR_CSTP cstp = new PLR_CSTP(temp);
        double MEP_SP = DlTools.getInstance().calculateMEPSP(temp, 20);
        double MEP_ES = DlTools.getInstance().calculateMEPES(MEP_SP, 2);
        List seg_CSTP2 = cstp.segmentBySLTP(MEP_SP, MEP_ES);
        double[] dataCSTP = cstp.getUpdateTSBySegPoints(seg_CSTP2);

        rCSTPData.put(index,dataCSTP);

        endTime = Instant.now();

        this._collector.ack(tuple);
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("SAXData","SAXIndex"));
    }

    public Instant getCSTPEndTime() {
        return endTime;
    }
}
