package com.outbrain.gruffalo.config;

import org.apache.commons.cli.*;

public class Config {

  public final int port;
  public final String[] graphiteClusters;
  public final int maxMetricLength;
  public final int flushOnIdleTime;
  public final int maxBatchSize;
  public final int reconnectOnIdleTime;
  public final int maxInflightBatches;

  private Config(final int port, final String[] graphiteClusters, int maxMetricLength, int flushOnIdleTime, int maxBatchSize, int reconnectOnIdleTime, int maxInflightBatches) {
    this.port = port;
    this.graphiteClusters = graphiteClusters;
    this.maxMetricLength = maxMetricLength;
    this.flushOnIdleTime = flushOnIdleTime;
    this.maxBatchSize = maxBatchSize;
    this.reconnectOnIdleTime = reconnectOnIdleTime;
    this.maxInflightBatches = maxInflightBatches;
  }

  public static Config parseCommand(final String appname, final String[] args) {
    return new ConfigParser(appname, args).parse();
  }

  private static class ConfigParser {

    private static final String OPTION_HELP = "help";
    private static final String OPTION_PORT = "port";
    private static final String OPTION_TARGETS = "clusters";
    private static final String OPTION_MAX_METRIC_LEN = "maxMetricLength";
    private static final String OPTION_FLUSH_ON_IDLE_TIME = "flushOnIdleTime";
    private static final String OPTION_MAX_BATCH_SIZE = "maxBatchSize";
    private static final String OPTION_RECONNECT_ON_IDLE_TIME = "reconnectOnIdleTime";
    private static final String OPTION_MAX_IN_FLIGHT_BATCHES = "maxInflightBatches";

    private final String appname;
    private final String[] args;
    private final Options options;

    private CommandLine cmd;

    ConfigParser(final String appname, final String[] args) {
      this.appname = appname;
      this.args = args;
      this.options = createCommandLineOptions();
    }

    private static Options createCommandLineOptions() {
      final Options options = new Options();
      options.addOption("h", OPTION_HELP, false, "print this message");
      options.addOption(Option.builder("p")
          .longOpt(OPTION_PORT)
          .required()
          .hasArg()
          .type(Number.class)
          .desc("TCP listen port (required)")
          .build());
      options.addOption(Option.builder("c")
          .longOpt(OPTION_TARGETS)
          .required()
          .hasArg()
          .desc("Graphite relay clusters (required)")
          .build());
      options.addOption(Option.builder(OPTION_MAX_METRIC_LEN)
          .hasArg()
          .type(Number.class)
          .desc("Maximum length of a frame we're willing to decode (default 4096)")
          .build());
      options.addOption(Option.builder(OPTION_FLUSH_ON_IDLE_TIME)
          .hasArg()
          .type(Number.class)
          .desc("Max amount of time (seconds) in which a channel can be idle before the current batch is flushed (default 10). 0 means disabled.")
          .build());
      options.addOption(Option.builder(OPTION_MAX_BATCH_SIZE)
          .hasArg()
          .type(Number.class)
          .desc("Max size of metrics batch in characters. " +
              "This setting affects the memory footprint of the server - each channel holds a buffer of the specified size for batching " +
              "(default 8192)")
          .build());
      options.addOption(Option.builder(OPTION_RECONNECT_ON_IDLE_TIME)
          .hasArg()
          .type(Number.class)
          .desc("Max time in seconds before we reconnect an idle downstream channel (default 120)")
          .build());
      options.addOption(Option.builder(OPTION_MAX_IN_FLIGHT_BATCHES)
          .hasArg()
          .type(Number.class)
          .desc("Max number of allowed in-flight batches before we start pushing back clients (default 1500)")
          .build());

      return options;
    }

    private Config parse() {
      try {
        maybeHelp();
        cmd = new DefaultParser().parse(options, args);

        final Number port = (Number) cmd.getParsedOptionValue(OPTION_PORT);
        final String[] graphiteClusters = cmd.getOptionValues(OPTION_TARGETS);
        final Number maxMetricLength = getOptionalNumber(OPTION_MAX_METRIC_LEN, 4096);
        final Number flushOnIdleTime = getOptionalNumber(OPTION_FLUSH_ON_IDLE_TIME, 10);
        final Number maxBatchSize = getOptionalNumber(OPTION_MAX_BATCH_SIZE, 8192);
        final Number reconnectOnIdleTime = getOptionalNumber(OPTION_RECONNECT_ON_IDLE_TIME, 120);
        final Number maxInflightBatches = getOptionalNumber(OPTION_MAX_IN_FLIGHT_BATCHES, 1500);

        return new Config(port.intValue(), graphiteClusters,
            maxMetricLength.intValue(), flushOnIdleTime.intValue(),
            maxBatchSize.intValue(), reconnectOnIdleTime.intValue(),
            maxInflightBatches.intValue());
      } catch (ParseException e) {

        System.err.println("Failed to parse command line: \n\t" + e.getMessage() + "\n");
        usage();
        return null;
      }

    }

    private void maybeHelp() throws ParseException {
      final Options helpOptions = new Options();
      helpOptions.addOption(options.getOption(OPTION_HELP));
      cmd = new DefaultParser().parse(helpOptions, args, true);

      if (cmd.hasOption(OPTION_HELP)) {
        usage();
      }
    }

    private void usage() {
      new HelpFormatter().printHelp(appname, options);
      System.exit(1);
    }

    private Number getOptionalNumber(final String opt, final Number defaultValue) throws ParseException {
      return cmd.hasOption(opt) ? (Number) cmd.getParsedOptionValue(opt) : defaultValue;
    }

  }
}
