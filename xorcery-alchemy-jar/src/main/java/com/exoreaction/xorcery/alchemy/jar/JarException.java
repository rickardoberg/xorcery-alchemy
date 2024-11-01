package com.exoreaction.xorcery.alchemy.jar;

public class JarException
    extends RuntimeException
{
    private final JarConfiguration jarConfiguration;
    private final RecipeConfiguration recipeConfiguration;

    public JarException(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration, String message) {
        super(message);
        this.jarConfiguration = jarConfiguration;
        this.recipeConfiguration = recipeConfiguration;
    }

    public JarException(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration, Throwable cause) {
        super(cause);
        this.jarConfiguration = jarConfiguration;
        this.recipeConfiguration = recipeConfiguration;
    }

    public JarException(JarConfiguration jarConfiguration, RecipeConfiguration recipeConfiguration, String message, Throwable cause) {
        super(message, cause);
        this.jarConfiguration = jarConfiguration;
        this.recipeConfiguration = recipeConfiguration;
    }

    @Override
    public String getMessage() {
        return recipeConfiguration.getName()+"."+jarConfiguration.getName()+":"+super.getMessage();
    }
}
