package de.yugata.editor.editor.filter;

import de.yugata.editor.editor.EditInfo;
import de.yugata.editor.editor.filter.filters.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FilterManager {

    private final Map<String, Class<?>> filters = new HashMap<>();

    public FilterManager() {
        this.populateFilters();
    }


    public void populateFilters() {
        final Class<?>[] classes = {
                FadeInAudio.class,
                FadeOutAudio.class,
                FadeOutVideo.class,
                InterpolateFilter.class,
                ZoomInFilter.class,
                FadeToBlackTransition.class,
                FadeIntroAudio.class,
        };


        for (final Class<?> aClass : classes) {
            final FilterInfo filterInfo = aClass.getAnnotation(FilterInfo.class);
            if (filterInfo == null) {
                System.err.println("Warning: No annotation found for class: " + aClass);
            }
            filters.put(filterInfo.name(), aClass);
        }
    }


    public List<TransitionVideoFilter> getTransitions(final List<String> filters, final EditInfo editInfo) {
        final List<TransitionVideoFilter> filterList = new ArrayList<>();

        filterForType(FilterType.TRANSITION, aClass -> {

            try {
                final TransitionVideoFilter transitionVideoFilter = (TransitionVideoFilter) aClass
                        .getDeclaredConstructor(EditInfo.class)
                        .newInstance(editInfo);

                if (filters.contains(transitionVideoFilter.getName()))
                    filterList.add(transitionVideoFilter);

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        return filterList;
    }


    public List<SimpleVideoFilter> getVideoFilters(final List<String> filters, final EditInfo editInfo) {
        final List<SimpleVideoFilter> filterList = new ArrayList<>();

        filterForType(FilterType.VIDEO, aClass -> {

            try {
                final SimpleVideoFilter simpleVideoFilter = (SimpleVideoFilter) aClass
                        .getDeclaredConstructor(EditInfo.class)
                        .newInstance(editInfo);

                if (filters.contains(simpleVideoFilter.getName()))
                    filterList.add(simpleVideoFilter);

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        return filterList;
    }

    public List<SimpleAudioFilter> getAudioFilters(final List<String> filters, final EditInfo editInfo) {
        final List<SimpleAudioFilter> filterList = new ArrayList<>();

        filterForType(FilterType.AUDIO, aClass -> {

            try {
                final SimpleAudioFilter simpleAudioFilter = (SimpleAudioFilter) aClass
                        .getDeclaredConstructor(EditInfo.class)
                        .newInstance(editInfo);

                if (filters.contains(simpleAudioFilter.getName()))
                    filterList.add(simpleAudioFilter);

            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
        return filterList;
    }


    private void filterForType(final FilterType type, final Consumer<Class<?>> classConsumer) {
        for (final Map.Entry<String, Class<?>> stringClassEntry : filters.entrySet()) {
            final FilterInfo filterInfo = stringClassEntry.getValue().getAnnotation(FilterInfo.class);

            if (filterInfo.filterType() == type) {
                classConsumer.accept(stringClassEntry.getValue());
            }
        }
    }
}
