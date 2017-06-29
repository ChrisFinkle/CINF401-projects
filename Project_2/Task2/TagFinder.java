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
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class TagFinder {

	public static class TagFinderMapper
			extends Mapper<Object, Text, Text, Text>{

			private Text site = new Text();
			private Text tag = new Text();

			private String tagRegex = "TagName=\"(\\w+)\"";
			private String countRegex = "Count=\"(\\d+)\"";
			private String siteRegex = "([\\.\\w]+)\\.com";

			private Pattern tagPattern = Pattern.compile(tagRegex);
			private Pattern countPattern = Pattern.compile(countRegex);
			private Pattern sitePattern = Pattern.compile(siteRegex);

			public void map(Object key, Text value, Context context) throws IOException, 
			       InterruptedException {

				       Matcher tagMatcher = tagPattern.matcher(value.toString());
				       Matcher countMatcher = countPattern.matcher(value.toString());
				       if(tagMatcher.find() && countMatcher.find()){
					       String tagstr = tagMatcher.group(1) + "," + 
						       countMatcher.group(1);
					       tag.set(tagstr);
				       } else {
					       tag.set("NA,-1");
				       }

				       String filePathString = ((FileSplit) 
						       context.getInputSplit()).getPath().toString();
				       Matcher siteMatcher = sitePattern.matcher(filePathString);

				       if(siteMatcher.find()){
					       String sitestr = siteMatcher.group(1);
					       site.set(sitestr);
				       }
				       else{
					       site.set("NA");
				       }
				       context.write(site, tag);

			}

	}

	public static class Top10Reducer
			extends Reducer<Text,Text,Text,Text> {
			private Text result = new Text();

			public void reduce(Text key, Iterable<Text> values,
					Context context
					) throws IOException, InterruptedException {
				String[] tags = new String[10];
				int[] counts = new int[10];
				for(Text val : values){
					int i = 0;
					int c;
					String st = val.toString().split(",")[1];
					if(st.matches("^-?\\d+$")){
						c = Integer.parseInt(st);
					}
					else {
						c = 0;
					}
					String tag = val.toString().split(",")[0];
					while(counts[i]>c && i++<9){}
					if(i<10){
						for(int j = 9; j>i; j--){
							counts[j] = counts [j-1];
							tags[j] = tags[j-1];
						}
						counts[i] = c;
						tags[i] = tag;
					}
				}
                String tagStr = String.join(",", tags);
				result.set(tagStr);
				context.write(key, result);
					}
	}


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
		conf.set("file.pattern", ".*(Tags\\.xml)");
		Job job = Job.getInstance(conf, "tag finder");
		job.setJarByClass(TagFinder.class);
		job.setMapperClass(TagFinderMapper.class);
		job.setReducerClass(Top10Reducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.setInputDirRecursive(job, true);
		FileInputFormat.setInputPathFilter(job, RegexPathFilter.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}

