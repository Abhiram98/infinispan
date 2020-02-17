package org.infinispan.anchored.configuration;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationInfo;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.elements.DefaultElementDefinition;
import org.infinispan.commons.configuration.elements.ElementDefinition;
import org.infinispan.configuration.serializing.SerializedWith;

@SerializedWith(AnchoredKeysConfigurationSerializer.class)
@BuiltBy(AnchoredKeysConfigurationBuilder.class)
public class AnchoredKeysConfiguration implements ConfigurationInfo {
   static final AttributeDefinition<Boolean> ENABLED =
         AttributeDefinition.builder("enabled", false).immutable().build();

   private final ElementDefinition<ConfigurationInfo> elementDefinition;
   private final AttributeSet attributes;

   public AnchoredKeysConfiguration(AttributeSet attributes) {
      this.elementDefinition = new DefaultElementDefinition<>(Element.ANCHORED_KEYS.getLocalName(), true, false);
      this.attributes = attributes;
   }

   public static AttributeSet attributeSet() {
      return new AttributeSet(AnchoredKeysConfiguration.class, ENABLED);
   }

   public AttributeSet attributes() {
      return attributes;
   }

   public boolean enabled() {
      return attributes.attribute(ENABLED).get();
   }

   @Override
   public ElementDefinition getElementDefinition() {
      return elementDefinition;
   }

   @Override
   public String toString() {
      return "AnchoredKeysConfiguration" + attributes.toString(null);
   }
}
