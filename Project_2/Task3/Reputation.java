import java.io.IOException;
import java.util.regex.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Reputation {

    public static class ReputationMapper extends Mapper<Object, Text, IntWritable, IntWritable>{

    	private IntWritable accountId = new IntWritable();
    	private IntWritable rep = new IntWritable();    	
    	
    	private String accIdRegex = "AccountId=\"(\\d+)\"";
    	private String repRegex = "Reputation=\"(\\d+)\"";
    	
    	private Pattern accIdPattern = Pattern.compile(accIdRegex);
    	private Pattern repPattern = Pattern.compile(repRegex);

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

    		Matcher accIdMatcher = accIdPattern.matcher(value.toString());
        	Matcher repMatcher = repPattern.matcher(value.toString());
        	
        	if(accIdMatcher.find()) {
        		int temp = Integer.parseInt(accIdMatcher.group(1));
        		accountId.set(temp);
        	} else {
        		accountId.set(-2);
        	}
        	
        	if(repMatcher.find()) {
        		int temp = Integer.parseInt(repMatcher.group(1));
        		rep.set(temp);
        	} else {
        		rep.set(-2);
        	}
        	
        	context.write(accountId, rep);
        
        }
        
    }

    public static class RepReducer extends Reducer<IntWritable,IntWritable,IntWritable,IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        	
        	int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        	
        }
    }
    
    //from: https://hadoopi.wordpress.com/2013/07/29/hadoop-filter-input-files-used-for-mapreduce/
    public static class RegexPathFilter extends Configured implements PathFilter {

        Pattern pattern;
        Configuration conf;
        FileSystem fs;

        @Override
        public boolean accept(Path path) {
            try {
                if (fs.isDirectory(path)) {
                    return true;
                } else {
                    Matcher m = pattern.matcher(path.toString());
                    System.out.println("Is path: " + path.toString() + " matches "
                            + conf.get("file.pattern") + " ? , " + m.matches());
                    return m.matches();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void setConf(Configuration conf) {
            this.conf = conf;
            if (conf != null) {
                try {
                    fs = FileSystem.get(conf);
                    if(conf.get("file.pattern") == null) {
                        conf.set("file.pattern", ".*");
                    }
                    pattern = Pattern.compile(conf.get("file.pattern"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set("file.pattern", ".*(Users\\.xml)");
        Job job = Job.getInstance(conf, "reputation");
        job.setJarByClass(Reputation.class);
        job.setMapperClass(ReputationMapper.class);
        job.setCombinerClass(RepReducer.class);
        job.setReducerClass(RepReducer.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.setInputDirRecursive(job, true);
        FileInputFormat.setInputPathFilter(job, RegexPathFilter.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}