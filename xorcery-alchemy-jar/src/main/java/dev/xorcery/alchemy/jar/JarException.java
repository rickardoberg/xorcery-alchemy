package dev.xorcery.alchemy.jar;

public class JarException
    extends RuntimeException
{
    private final JarConfiguration jarConfiguration;
    private final TransmutationConfiguration transmutationConfiguration;

    public JarException(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration, String message) {
        super(message);
        this.jarConfiguration = jarConfiguration;
        this.transmutationConfiguration = transmutationConfiguration;
    }
    public JarException(JarConfiguration jarConfiguration, TransmutationConfiguration transmutationConfiguration, String message, Throwable cause) {
        super(message, cause);
        this.jarConfiguration = jarConfiguration;
        this.transmutationConfiguration = transmutationConfiguration;
    }

    @Override
    public String getMessage() {
        return transmutationConfiguration.getName().map(tn -> tn+".").orElse("")+jarConfiguration.getName().orElseGet(jarConfiguration::getJar)+":"+super.getMessage();
    }
}
