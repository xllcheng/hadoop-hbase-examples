package com.adintellig.hbase.userattrlib;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import com.adintellig.util.ConfigFactory;
import com.adintellig.util.ConfigProperties;
import com.adintellig.util.Const;

public class Main {
	static final Log LOG = LogFactory.getLog(Main.class);

	public static final String NAME = "User Attribute Lib";
	public static final String TEMP_INDEX_PATH = "/tmp/user-attr-lib-gender-age";
	public static String inputTable = "user_behavior_attribute_noregistered";

	static ConfigProperties config = ConfigFactory.getInstance()
			.getConfigProperties(ConfigFactory.APP_CONFIG_PATH);

	public static void main(String[] args) throws Exception {
		Configuration conf = HBaseConfiguration.create();

		Scan scan = new Scan();
		/* batch and caching */
		scan.setBatch(0);
		scan.setCaching(10000);
		scan.setMaxVersions(1);
		/* configure scan */

		scan.addColumn(Bytes.toBytes("attr"), Bytes.toBytes("gender"));
		scan.addColumn(Bytes.toBytes("attr"), Bytes.toBytes("age"));

		// hbase master
		conf.set(ConfigProperties.CONFIG_NAME_HBASE_MASTER,
				config.getProperty(ConfigProperties.CONFIG_NAME_HBASE_MASTER));
		// zookeeper quorum
		conf.set(
				ConfigProperties.CONFIG_NAME_HBASE_ZOOKEEPER_QUORUM,
				config.getProperty(ConfigProperties.CONFIG_NAME_HBASE_ZOOKEEPER_QUORUM));

		// set hadoop speculative execution to false
		conf.setBoolean(Const.HADOOP_MAP_SPECULATIVE_EXECUTION, false);
		conf.setBoolean(Const.HADOOP_REDUCE_SPECULATIVE_EXECUTION, false);

		Path tempIndexPath = new Path(TEMP_INDEX_PATH);
		FileSystem fs = FileSystem.get(conf);
		if (fs.exists(tempIndexPath)) {
			fs.delete(tempIndexPath, true);
		}

		/* JOB-1: generate secondary index */
		Job job = new Job(conf, NAME);
		job.setJarByClass(Main.class);

		TableMapReduceUtil.initTableMapperJob(inputTable, scan,
				UidGenderAgeMapper.class, Text.class, Text.class, job);
		job.setNumReduceTasks(0);
		job.setOutputFormatClass(TextOutputFormat.class);
		FileOutputFormat.setOutputPath(job, tempIndexPath);

		int success = job.waitForCompletion(true) ? 0 : 1;

		System.exit(success);
	}

}
