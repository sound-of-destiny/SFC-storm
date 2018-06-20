import bolt.CSTPBolt;
import bolt.FSWBolt;
import bolt.SAXBolt;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;
import spout.SFCDataSpout;
import util.DlTools;
import utilities.ClassifierTools;
import weka.core.Instances;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SFCTopology {
    private static final String SFCData_Spout = "SFCDataSpout";
    private static final String SAX_Bolt = "SAXBolt";
    private static final String FSW_Bolt = "FSWBolt";
    private static final String CSTP_Bolt = "CSTPBolt";
    private static final String TOPOLOGY_NAME = "SFCTopology";
    public static void main(String[] args) throws Exception {

        DlTools.getInstance().deleteResultFiles();
        Log log = LogFactory.get();
        DecimalFormat df = new DecimalFormat("#.########");
        ConcurrentHashMap<Integer,double[]> SAXData = new ConcurrentHashMap<>();
        int count = 0;
        List<String> arffFiles = DlTools.getInstance().getArffFiles();
        for (String arffFilePath : arffFiles) {
            Instances test = ClassifierTools.loadData(arffFilePath);
            int numInstance = test.numInstances();
            for (int i = 0; i<numInstance; i++){
                double[] data = test.instance(i).toDoubleArray();
                SAXData.put(i+count,data);
            }
            count += numInstance;
            log.info( numInstance+"" );
        }

        SFCDataSpout spout = new SFCDataSpout(SAXData);
        SAXBolt sax = new SAXBolt();
        FSWBolt fsw = new FSWBolt();
        CSTPBolt cstp = new CSTPBolt();

        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(SFCData_Spout,spout,8);
        builder.setBolt(SAX_Bolt,sax,8).fieldsGrouping(SFCData_Spout,new Fields("SAXData","SAXIndex"));
        builder.setBolt(FSW_Bolt,fsw,8).fieldsGrouping(SFCData_Spout,new Fields("SAXData","SAXIndex"));
        builder.setBolt(CSTP_Bolt,cstp,8).fieldsGrouping(SFCData_Spout,new Fields("SAXData","SAXIndex"));

        Config config = new Config();
        config.setDebug(true);
        LocalCluster cluster = new LocalCluster();

        Instant startTime = Instant.now();

        cluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());

        Utils.sleep(60000);
        Instant LStartTime = spout.getStartTime();
        Instant saxEndTime = sax.getSAXEndTime();
        Instant fswEndTime = fsw.getFSWEndTime();
        Instant cstpEndTime = cstp.getCSTPEndTime();

        long saxTime = Duration.between(LStartTime,saxEndTime).toMillis();
        long fswTime = Duration.between(LStartTime,fswEndTime).toMillis();
        long cstpTime = Duration.between(LStartTime,cstpEndTime).toMillis();

        ConcurrentHashMap<Integer,Object[]> rSAXData = sax.getrSAXData();
        ConcurrentHashMap<Integer,double[]> rFSWData = fsw.getrFSWData();
        ConcurrentHashMap<Integer,double[]> rCSTPData = cstp.getrCSTPData();

        StringBuilder sb = new StringBuilder();
        for(int i=0; i<count; i++){

            int dataEndF = (int) rSAXData.get(i)[0];
            double[] dataF = (double[])rSAXData.get(i)[1];
            String[] dataSAXF = (String[]) rSAXData.get(i)[2];
            double[] dataFSWF = rFSWData.get(i);
            double[] dataCSTPF = rCSTPData.get(i);
            sb.append(dataEndF+1);
            sb.append("\t");
            int length = dataSAXF.length;
            for(int j=0; j<length;j++){
                sb.append(dataF[j]);
                if(j!=length-1) sb.append(",");
            }
            sb.append("\t");
            for(int j=0; j<length;j++){
                sb.append(dataSAXF[j]);
                if(j!=length-1) sb.append(",");
            }
            sb.append("\t");
            for(int j=0; j<length;j++){
                sb.append(df.format(dataFSWF[j]));
                if(j!=length-1) sb.append(",");
            }
            sb.append("\t");
            for(int j=0; j<length;j++){
                sb.append(df.format(dataCSTPF[j]));
                if(j!=length-1) sb.append(",");
            }
            sb.append('\n');
        }
        DlTools.getInstance().stringToFile( "test_data.txt", sb.toString());
        DlTools.getInstance().stringToFile( "test_time_.txt", "saxTime:"+saxTime+" fswTime:"+fswTime+" cstpTime:"+cstpTime);
        cluster.killTopology(TOPOLOGY_NAME);
        cluster.shutdown();
    }
}
