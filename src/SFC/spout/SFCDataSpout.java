package spout;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SFCDataSpout extends BaseRichSpout {
    private ConcurrentHashMap<Integer,double[]> SAXData;

    public SFCDataSpout(ConcurrentHashMap<Integer,double[]> SAXData){
        this.SAXData = SAXData;
    }
    private SpoutOutputCollector _collector;
    private int index = 0;
    private static Instant startTime;
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        this._collector = collector;
    }

    public void nextTuple() {
        if (index == 0){
            startTime = Instant.now();
        }
        int saxSize = SAXData.size();
        if(saxSize > index){
            double[] data = this.SAXData.get(index);
            this._collector.emit(new Values(data,index));
            index++;
        }
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("SAXData","SAXIndex"));
    }

    public Instant getStartTime() {
        return startTime;
    }
}
