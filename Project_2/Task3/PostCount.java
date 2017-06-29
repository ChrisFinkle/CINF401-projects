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
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class PostCount {

    public static class PostCountMapper extends Mapper<Object, Text, Text, IntWritable>{

    	private Text userId = new Text();
        private final static IntWritable one = new IntWritable(1);
    	
    	private String userIdRegex = "UserId=\"(\\d+)\"";
    	private String siteRegex = "([\\.\\w]+)\\.com";	
    	private Pattern userIdPattern = Pattern.compile(userIdRegex);
    	private Pattern sitePattern = Pattern.compile(siteRegex);
    	public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        		
        	Matcher userIdMatcher = userIdPattern.matcher(value.toString());
        	String filePathString = ((FileSplit)context.getInputSplit()).getPath().toString();
        	Matcher siteMatcher = sitePattern.matcher(filePathString);
        	
        	if(userIdMatcher.find() && siteMatcher.find()) {
			String temp = siteMatcher.group(1)+","+userIdMatcher.group(1);
        		userId.set(temp);
        	} else {
        		userId.set("NA,-2");
        	}
        	
        	context.write(userId, one);
        		
        }
        
    }

    public static class PostCountReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
    	
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        	
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
        conf.set("file.pattern", ".*(Posts\\.xml)");
        Job job = Job.getInstance(conf, "post count");
        job.setJarByClass(PostCount.class);
        job.setMapperClass(PostCountMapper.class);
	job.setCombinerClass(PostCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.setInputDirRecursive(job, true);
        FileInputFormat.setInputPathFilter(job, RegexPathFilter.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
