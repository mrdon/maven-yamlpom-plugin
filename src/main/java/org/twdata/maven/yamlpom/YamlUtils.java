package org.twdata.maven.yamlpom;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 *
 */
public class YamlUtils
{
    public static Yaml buildYaml()
    {
        Loader myLoader = new Loader();
        Yaml yaml = new Yaml(myLoader);

        // Don't let the YAML parser try to guess things.  Will screw up things like version numbers that look like
        // 1.00 by converting them to an int "1.0"
        myLoader.setResolver(new Resolver()
        {
            @Override
            public String resolve(NodeId kind, String value, boolean implicit)
            {
                String tag = super.resolve(kind, value, implicit);
                if (implicit)
                {
                    if (tag.equals("tag:yaml.org,2002:bool") ||
                        tag.equals("tag:yaml.org,2002:float") ||
                        tag.equals("tag:yaml.org,2002:int") ||
                        tag.equals("tag:yaml.org,2002:timestamp") ||
                        tag.equals("tag:yaml.org,2002:value"))
                    {
                        return "tag:yaml.org,2002:str";
                    }
                }
                return tag;
            }
        });
        return yaml;
    }
}
