package amtgroup.devinfra.crowdldap.component.crowd.query.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Vitaly Ogoltsov
 */
@Component
@Slf4j
public class CrowdLdapFilter {

    public Optional<Predicate<ServerEntry>> of(ExprNode filter) {
        if (filter instanceof AndNode) {
            List<Predicate<ServerEntry>> predicates = ((AndNode) filter).getChildren()
                    .stream()
                    .map(this::of)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            return predicates.isEmpty()
                    ? Optional.empty()
                    : Optional.of(t -> predicates.stream().allMatch(p -> p.test(t)));
        } else if (filter instanceof OrNode) {
            List<Predicate<ServerEntry>> predicates = ((OrNode) filter).getChildren()
                    .stream()
                    .map(this::of)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            return predicates.isEmpty()
                    ? Optional.empty()
                    : Optional.of(t -> predicates.stream().anyMatch(p -> p.test(t)));
        } else if (filter instanceof NotNode) {
            return of(((NotNode) filter).getFirstChild())
                    .map(Predicate::negate);
        } else if (filter instanceof EqualityNode) {
            EqualityNode node = (EqualityNode) filter;
            return Optional.of(eq(node.getAttribute(), node.getValue()));
        } else if (filter instanceof GreaterEqNode) {
            GreaterEqNode node = (GreaterEqNode) filter;
            return Optional.of(goe(node.getAttribute(), node.getValue()));
        } else if (filter instanceof LessEqNode) {
            LessEqNode node = (LessEqNode) filter;
            return Optional.of(loe(node.getAttribute(), node.getValue()));
        }
        log.warn("Expression nodes of type [{}] are not supported", filter.getClass().getSimpleName());
        return Optional.empty();
    }


    private Predicate<ServerEntry> eq(String attributeName, Value attributeValue) {
        return e -> {
            EntryAttribute attribute = e.get(attributeName);
            if (attribute == null) {
                return false;
            }
            if (Objects.equals(attributeValue.getString(), "*")) {
                return true;
            }
            for (Iterator<Value<?>> valueIterator = attribute.getAll(); valueIterator.hasNext(); ) {
                Value<?> value = valueIterator.next();
                if (Objects.equals(value, attributeValue)) {
                    return true;
                }
            }
            return false;
        };
    }

    @SuppressWarnings("unchecked")
    private Predicate<ServerEntry> goe(String attributeName, Value attributeValue) {
        return e -> {
            EntryAttribute attribute = e.get(attributeName);
            if (attribute == null) {
                return false;
            }
            for (Iterator<Value<?>> valueIterator = attribute.getAll(); valueIterator.hasNext(); ) {
                Value<?> value = valueIterator.next();
                if (ObjectUtils.compare(value, attributeValue) >= 0) {
                    return true;
                }
            }
            return false;
        };
    }

    @SuppressWarnings("unchecked")
    private Predicate<ServerEntry> loe(String attributeName, Value attributeValue) {
        return e -> {
            EntryAttribute attribute = e.get(attributeName);
            if (attribute == null) {
                return false;
            }
            for (Iterator<Value<?>> valueIterator = attribute.getAll(); valueIterator.hasNext(); ) {
                Value<?> value = valueIterator.next();
                if (ObjectUtils.compare(value, attributeValue) <= 0) {
                    return true;
                }
            }
            return false;
        };
    }

}
