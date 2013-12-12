package com.naleid.builder

import org.apache.log4j.Logger
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.metamodel.EntityType
import javax.persistence.metamodel.PluralAttribute
import javax.persistence.metamodel.SingularAttribute
import java.beans.Introspector
import java.lang.reflect.ParameterizedType

class EntityBuilder<E> {

    private static final Logger LOG = Logger.getLogger(EntityBuilder.class);

    @PersistenceContext
    EntityManager entityManager

    @Autowired
    protected EntityBuilderLocator entityBuilderLocator

    private final Class<E> entityClass

    protected Map<String, Object> calculatedDefaultProperties = [:]
    protected Map<String, EntityBuilder> calculatedDefaultAssociations = [:]

    public EntityBuilder() {
        this.entityClass = (Class<E>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public EntityBuilder(EntityManager overrideEntityManager, EntityBuilderLocator locator) {
        this()
        this.entityManager = overrideEntityManager
        this.entityBuilderLocator = locator
        cacheEntityMetadata()
    }

    public EntityBuilder(Class clazz, EntityManager overrideEntityManager, EntityBuilderLocator locator) {
        this.entityManager = overrideEntityManager
        this.entityClass = clazz
        this.entityBuilderLocator = locator
        cacheEntityMetadata()
    }

    E build(Map properties = [:]) {
        E entity = newInstance(properties)
        return save(entity)
    }

    E save(E entity) {
        final E savedEntity = entityManager.merge(entity)
        return savedEntity
    }

    E newInstance(Map properties) {
        Map mergedProperties = resolveProperties(properties)
        E entity = entityClass.newInstance(mergedProperties)
        return entity
    }

    Map resolveProperties(Map properties) {
        Map merged = (calculatedDefaultProperties + defaultProperties + properties).collectEntries { k, v ->
            // lets us use a closure as a value that we can exercise to determine value at runtime
            switch (v.class) {
                case Closure:
                    return [k, v.call()]
                default:
                    return [k, v]
            }
        }

        (calculatedDefaultAssociations + defaultAssociations).each { String property, EntityBuilder builder ->
            if (!merged.containsKey(property)) merged[property] = builder.build()
        }

        return merged
    }

    // for subclasses to @Override to control what property values are
    Map<String, Object> getDefaultProperties() { [:] }

    // for subclasses to @Override to control what property values are
    Map<String, EntityBuilder> getDefaultAssociations() { [:] }

    @PostConstruct
    public void cacheEntityMetadata() {
        EntityType<E> entityType = entityManager.entityManagerFactory.metamodel.entity(entityClass)
        cacheDefaults(entityType)
    }

    public void cacheDefaults(EntityType<E> entityType) {
        cacheDefaultProperties(entityType)
        cacheDefaultAssociations(entityType)
    }

    // add any missing properties to the default properties collection
    public void cacheDefaultProperties(EntityType<E> entityType) {
        HashSet<SingularAttribute<? super E, ?>> requiredAttributes = findRequiredProperties(entityType)

        for (SingularAttribute attribute : requiredAttributes) {
            def basicValue = determineBasicValue(attribute.name, attribute.javaType)
            if (basicValue != null) {
                calculatedDefaultProperties[attribute.name] = basicValue
            } else {
                calculatedDefaultAssociations[attribute.name] = determineBuilder(attribute.javaType)
            }
        }
    }

    EntityBuilder determineBuilder(Class classNeedingBuilder) {
        return determineBuilder(classNeedingBuilder, entityManager, entityBuilderLocator)
    }

    static EntityBuilder determineBuilder(Class classNeedingBuilder, EntityManager entityManager, EntityBuilderLocator locator) {
        EntityBuilder builder = null
        String beanName = classToBuilderBeanName(classNeedingBuilder)

        if (locator) {
            try {
                builder = locator.lookup(beanName)
            } catch (NoSuchBeanDefinitionException nsbde) { /* ignore */}
        }

        if (!builder) {
            LOG.debug("spring doesn't know about a builder named $beanName, instantiate a new one with all defaults")
            builder = new EntityBuilder(classNeedingBuilder, entityManager, locator)
        }
        return builder
    }

    protected static String classToBuilderBeanName(Class classNeedingBuilder) {
        Introspector.decapitalize(classNeedingBuilder.simpleName) + "Builder"
    }

    protected HashSet<SingularAttribute<? super E, ?>> findRequiredProperties(EntityType<E> entityType) {
        // find all the attributes that we haven't been given by a subclass and
        // that we're required to calculate, special exclusion for created & updated
        // as those can't (apparently) be detected in the JPA API
        return entityType.singularAttributes.findAll { SingularAttribute attribute ->
            String name = attribute.name
            !this.defaultProperties.hasProperty(name) &&
                    !this.defaultAssociations.hasProperty(name) &&
                    !attribute.isOptional() &&
                    !attribute.isId() &&
                    !attribute.isVersion() &&
                    name != "created" &&
                    name != "updated"
        }
    }

    protected Object determineBasicValue(propertyName, Class propertyType) {
        switch(propertyType) {
            case String:
                return propertyName
            case Calendar:
                return new GregorianCalendar()
            case Currency:
                return Currency.getInstance(Locale.default)
            case TimeZone:
                return TimeZone.default
            case Locale:
                return Locale.default
            case java.sql.Date:
                return new java.sql.Date(new Date().time)
            case java.sql.Time:
                return new java.sql.Time(new Date().time)
            case Date:
                return new Date()
            case Boolean:
            case boolean:
                return false
            case { it.isPrimitive() || it.isAssignableFrom(Number.class)}:
                return 0
            case Byte[]:
            case byte[]:
                // this is the binary for a tiny little gif image
                byte[] inputBytes = [71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -111, -1, 0, -1, -1, -1, 0, 0, 0, -1, -1, -1, 0, 0, 0, 33, -1, 11, 65, 68, 79, 66, 69, 58, 73, 82, 49, 46, 48, 2, -34, -19, 0, 33, -7, 4, 1, 0, 0, 2, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 84, 1, 0, 59]
                return inputBytes
            case Enum:
                return propertyType.values()[0]
            default:
                LOG.debug("Unable to determine basic value for $propertyName of type $propertyType, must be related class")
                return null
        }
    }

    public void cacheDefaultAssociations(EntityType<E> entityType) {
        Set<PluralAttribute> pluralAttributes = entityType.declaredPluralAttributes
        // todo: how can we find (through spring?) a builder for the pluralAttributes?
        // is this worth doing generically?
    }
}

