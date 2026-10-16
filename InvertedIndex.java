import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InvertedIndex {

    public static class IndexMapper extends Mapper<Object, Text, Text, Text> {
        private Text wordKey = new Text();
        private Text docVal = new Text();
        private Map<String, Boolean> stopWords = new HashMap<>();

        @Override
        protected void setup(Context context) throws IOException {
            // 加载停用词
            BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String w = line.trim().toLowerCase();
                if (!w.isEmpty()) {
                    stopWords.put(w, true);
                }
            }
            br.close();
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            FileSplit split = (FileSplit) context.getInputSplit();
            String docName = split.getPath().getName();
            String content = value.toString().toLowerCase();
            // 清除引号，分词
            String[] words = content.replaceAll("[\"']", "").split("[\\s,.!?();:\\[\\]]+");

            for (String w : words) {
                w = w.trim();
                if (w.isEmpty() || stopWords.containsKey(w)) continue;
                // key：单词；value：文档名
                wordKey.set(w);
                docVal.set(docName);
                context.write(wordKey, docVal);
            }
        }
    }

    public static class IndexReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // 统计每个文档的词频
            Map<String,Integer> docCountMap = new HashMap<>();
            int total = 0;
            for (Text val : values) {
                String doc = val.toString();
                int cnt = docCountMap.getOrDefault(doc, 0)+1;
                docCountMap.put(doc, cnt);
                total++;
            }

            StringBuilder sb = new StringBuilder("<");
            // 拼接 <doc,count>;
            boolean first = true;
            for(Map.Entry<String,Integer> entry : docCountMap.entrySet()){
                if(!first){
                    sb.append(";");
                }
                first = false;
                sb.append(entry.getKey()).append(",").append(entry.getValue());
            }
            // 追加 total
            sb.append(";<total,").append(total).append(">.");
            Text outVal = new Text(sb.toString());
            context.write(key, outVal);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "InvertedIndex");
        job.setJarByClass(InvertedIndex.class);

        job.setMapperClass(IndexMapper.class);
        job.setReducerClass(IndexReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.addCacheFile(new Path("/user/hadoop/inverted/stopwords.txt").toUri());
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
