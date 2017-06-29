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

public class AgeFinder {

    public static class AgeFinderMapper extends Mapper<Object, Text, IntWritable, IntWritable>{

    	private IntWritable age = new IntWritable();
    	private IntWritable accountId = new IntWritable();
    	
    	private String ageRegex = "Age=\"(\\d+)\"";
    	private String idRegex = "AccountId=\"(\\d+)\"";
    	
    	private Pattern agePattern = Pattern.compile(ageRegex);
    	private Pattern idPattern = Pattern.compile(idRegex);

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        	Matcher ageMatcher = agePattern.matcher(value.toString());
        	Matcher idMatcher = idPattern.matcher(value.toString());
        	
        	if(ageMatcher.find()) {
        		int temp = Integer.parseInt(ageMatcher.group(1));
        		age.set(temp);
        	} else {
        		age.set(-1);
        	}
        	
        	if(idMatcher.find()) {
        		int temp = Integer.parseInt(idMatcher.group(1));
        		accountId.set(temp);
        	} else {
        		accountId.set(-2);
        	}
        	
        	context.write(accountId, age);

        }
        
    }

    public static class IdentReducer extends Reducer<IntWritable,IntWritable,IntWritable,IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            for(IntWritable val : values){
            	result.set(val.get());
            }
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
        Job job = Job.getInstance(conf, "age finder");
        job.setJarByClass(AgeFinder.class);
        job.setMapperClass(AgeFinderMapper.class);
        job.setCombinerClass(IdentReducer.class);
        job.setReducerClass(IdentReducer.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.setInputDirRecursive(job, true);
        FileInputFormat.setInputPathFilter(job, RegexPathFilter.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}