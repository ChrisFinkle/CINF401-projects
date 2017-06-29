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
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class UserAccount {

    public static class UserAccountMapper extends Mapper<Object, Text, IntWritable, Text>{

    	private Text userId = new Text();
    	private IntWritable accountId = new IntWritable();

    	private String userIdRegex = "Id=\"(\\d+)\"";
    	private String accIdRegex = "AccountId=\"(\\d+)\"";
		private String siteRegex = "([\\.\\w]+)\\.com";
    	
    	private Pattern userIdPattern = Pattern.compile(userIdRegex);
    	private Pattern accIdPattern = Pattern.compile(accIdRegex);
		private Pattern sitePattern = Pattern.compile(siteRegex);

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        	        		
        	Matcher accIdMatcher = accIdPattern.matcher(value.toString());
        	Matcher userIdMatcher = userIdPattern.matcher(value.toString());
	
        	if(accIdMatcher.find()) {
        		int temp = Integer.parseInt(accIdMatcher.group(1));
        		accountId.set(temp);
        	} else {
        		accountId.set(-2);
        	}
	
    		String filePathString = ((FileSplit)context.getInputSplit()).getPath().toString();
    		Matcher siteMatcher = sitePattern.matcher(filePathString);
    		
        	if(userIdMatcher.find() && siteMatcher.find()) {
        		String temp = siteMatcher.group(1)+","+userIdMatcher.group(1);
        		userId.set(temp);
        	} else {
        		userId.set("NA,-2");
        	}
		
			context.write(accountId, userId);
		            	
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
        Job job = Job.getInstance(conf, "post count");
        job.setJarByClass(UserAccount.class);
        job.setMapperClass(UserAccountMapper.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.setInputDirRecursive(job, true);
        FileInputFormat.setInputPathFilter(job, RegexPathFilter.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}