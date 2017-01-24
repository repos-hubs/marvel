package com.mirhoseini.marvel.character.search;

import com.mirhoseini.marvel.util.SchedulerProvider;
import com.mirhoseini.marvel.util.StateManager;

import javax.inject.Inject;

import rx.subscriptions.CompositeSubscription;

/**
 * Listens to user actions from the UI ({@link SearchContract.View}), retrieves the data and updates
 * the UI as required.
 */
class SearchPresenter implements SearchContract.Presenter {

    private SchedulerProvider scheduler;
    private SearchContract.Interactor interactor;
    private SearchContract.View view;
    private StateManager stateManager;
    private CompositeSubscription subscriptions = new CompositeSubscription();

    @Inject
    public SearchPresenter(SchedulerProvider scheduler, SearchContract.Interactor interactor, StateManager stateManager) {
        this.scheduler = scheduler;
        this.interactor = interactor;
        this.stateManager = stateManager;
    }

    @Override
    public void bind(SearchContract.View view) {
        this.view = view;
    }


    @Override
    public void doSearch(String query, long timestamp) {
        if (null != view) {
            view.showProgress();
        }

        subscriptions.add(
                interactor.loadCharacter(query, timestamp)
                        .observeOn(scheduler.mainThread())
                        .subscribe(character -> {
                                    if (null != view) {
                                        view.hideProgress();
                                        view.showCharacter(character);

                                        if (!stateManager.isConnect())
                                            view.showOfflineMessage(false);
                                    }
                                },
                                // handle exceptions
                                throwable -> {
                                    if (null != view) {
                                        view.hideProgress();

                                        if (stateManager.isConnect()) {
                                            if (throwable instanceof ApiResponseCodeException)
                                                view.showServiceError((ApiResponseCodeException) throwable);
                                            else if (throwable instanceof NoSuchCharacterException)
                                                view.showQueryNoResult();
                                            else
                                                view.showError(throwable);

                                        } else {
                                            view.showOfflineMessage(true);
                                        }
                                    }
                                }));
    }

    @Override
    public boolean isQueryValid(String query) {
        return null != query && !query.isEmpty();
    }

    @Override
    public void loadCharactersCachedData() {
        subscriptions.add(
                interactor.loadAllCharacters()
                        .observeOn(scheduler.mainThread())
                        .subscribe(characters -> {
                                    if (null != view)
                                        view.setCharactersCachedData(characters);
                                },
                                // handle database exception
                                throwable -> {
                                    if (null != view) {
                                        view.showDatabaseError(throwable);
                                    }
                                }));
    }

    @Override
    public void unbind() {
        subscriptions.clear();
        interactor.unbind();
        view = null;
    }
}
