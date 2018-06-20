    package bolt;

    import cn.hutool.log.Log;
    import cn.hutool.log.LogFactory;
    import org.apache.storm.task.OutputCollector;
    import org.apache.storm.task.TopologyContext;
    import org.apache.storm.topology.OutputFieldsDeclarer;
    import org.apache.storm.topology.base.BaseRichBolt;
    import org.apache.storm.tuple.Fields;
    import org.apache.storm.tuple.Tuple;

    import java.time.Instant;
    import java.util.Map;
    import java.util.concurrent.ConcurrentHashMap;

    public class SAXBolt extends BaseRichBolt {
        private Log log = LogFactory.get();
        private OutputCollector _collector;
        private static ConcurrentHashMap<Integer,Object[]> rSAXData = new ConcurrentHashMap<>();
        public ConcurrentHashMap<Integer,Object[]> getrSAXData(){
            return rSAXData;
        }
        public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
            this._collector=collector;
        }
        private static final String[] alphabetSymbols = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};

        private static Instant endTime;
        public void execute(Tuple tuple) {

            double[] data = (double[])tuple.getValue(0);
            int index = tuple.getInteger(1);

            int interval = data.length-1;
            int dataEnd = Integer.parseInt(new java.text.DecimalFormat("0").format(data[interval]));

            //start
            double[] temp = new double[interval];
            System.arraycopy(data, 0, temp, 0, interval);
            String[] dataSAX = convertSequence(temp, interval);

            Object[] dataFinal = new Object[5];
            dataFinal[0] = dataEnd;
            dataFinal[1] = data;
            dataFinal[2] = dataSAX;
            rSAXData.put(index,dataFinal);

            endTime = Instant.now();
            this._collector.ack(tuple);
        }

        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("SAXData","SAXIndex"));
        }

        public Instant getSAXEndTime() {
            return endTime;
        }

        private String[] convertSequence(double[] data, int interval) {
            double[] gaussianBreakpoints = generateBreakpoints(10);
            String[] dataString = new String[interval];
            for(int i = 0; i < interval; ++i) {
                for(int j = 0; j < 10; ++j) {
                    if (data[i] < gaussianBreakpoints[j]) {
                        data[i] = (double)j;
                        dataString[i]=alphabetSymbols[j];
                        break;
                    }
                }
            }
            return dataString;
        }

        private double[] generateBreakpoints(int alphabetSize) {
            double maxVal = 1.7976931348623157E308D;
            double[] breakpoints = null;
            switch(alphabetSize) {
                case 2:
                    breakpoints = new double[]{0.0D, maxVal};
                    break;
                case 3:
                    breakpoints = new double[]{-0.43D, 0.43D, maxVal};
                    break;
                case 4:
                    breakpoints = new double[]{-0.67D, 0.0D, 0.67D, maxVal};
                    break;
                case 5:
                    breakpoints = new double[]{-0.84D, -0.25D, 0.25D, 0.84D, maxVal};
                    break;
                case 6:
                    breakpoints = new double[]{-0.97D, -0.43D, 0.0D, 0.43D, 0.97D, maxVal};
                    break;
                case 7:
                    breakpoints = new double[]{-1.07D, -0.57D, -0.18D, 0.18D, 0.57D, 1.07D, maxVal};
                    break;
                case 8:
                    breakpoints = new double[]{-1.15D, -0.67D, -0.32D, 0.0D, 0.32D, 0.67D, 1.15D, maxVal};
                    break;
                case 9:
                    breakpoints = new double[]{-1.22D, -0.76D, -0.43D, -0.14D, 0.14D, 0.43D, 0.76D, 1.22D, maxVal};
                    break;
                case 10:
                    breakpoints = new double[]{-1.28D, -0.84D, -0.52D, -0.25D, 0.0D, 0.25D, 0.52D, 0.84D, 1.28D, maxVal};
                    break;
                default:
                    log.info("No breakpoints stored for alphabet size " + alphabetSize);
            }

            return breakpoints;
        }
    }
