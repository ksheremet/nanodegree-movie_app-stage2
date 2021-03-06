package ch.sheremet.katarina.movieapp.listmovies;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.sheremet.katarina.movieapp.R;
import ch.sheremet.katarina.movieapp.di.DaggerMovieMainComponent;
import ch.sheremet.katarina.movieapp.di.MovieMainComponent;
import ch.sheremet.katarina.movieapp.di.MovieMainModule;
import ch.sheremet.katarina.movieapp.favouritemovies.data.FavouriteMoviesUtil;
import ch.sheremet.katarina.movieapp.favouritemovies.data.MoviesContract;
import ch.sheremet.katarina.movieapp.model.Movie;
import ch.sheremet.katarina.movieapp.moviedetail.MovieDetailActivity;

public class MovieMainActivity extends AppCompatActivity
        implements MovieAdapter.MovieAdapterOnClickHandler,
        IMovieMainView,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int MOVIE_LOADER_ID = 123;
    private static final String RECYCLER_VIEW_STATE = "recycler_view_state";
    @BindView(R.id.movie_rv)
    RecyclerView mMoviesRecyclerView;
    @BindView(R.id.error_message_tv)
    TextView mErrorMessage;
    @BindView(R.id.loading_movies_pb)
    ProgressBar mLoadingMoviesIndicator;
    @Inject
    IMovieMainPresenter mPresenter;
    private MovieAdapter mMovieAdapter;
    private SharedPreferences mMoviePref;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_main);
        ButterKnife.bind(this);
        int spanCount = getResources().getInteger(R.integer.grid_span_count);
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        mMoviesRecyclerView.setLayoutManager(layoutManager);
        mMoviesRecyclerView.setHasFixedSize(true);
        mMovieAdapter = new MovieAdapter(this);
        mMoviesRecyclerView.setAdapter(mMovieAdapter);

        mMoviePref = getPreferences(Context.MODE_PRIVATE);

        MovieMainComponent component = DaggerMovieMainComponent.builder()
                .movieMainModule(new MovieMainModule(this)).build();
        component.injectMovieMainActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String movie_pref = mMoviePref.getString(getString(R.string.movie_pref_key),
                getString(R.string.popular_movies_pref));
        loadMoviesData(movie_pref);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(RECYCLER_VIEW_STATE, mMoviesRecyclerView
                .getLayoutManager().onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Use delayed runnable.
        // https://discussions.udacity.com/t/trouble-maintaining-recyclerview-position-upon-orientation-change/608216/12
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (savedInstanceState != null) {
                    Parcelable savedRecyclerLayoutState =
                            savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
                    mMoviesRecyclerView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
                }
            }
        }, 400);
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void loadMoviesData(String movie_pref) {
        mLoadingMoviesIndicator.setVisibility(View.VISIBLE);
        if (!isOnline()) {
            showErrorMessage(getString(R.string.offline_user_message));
            return;
        }
        if (movie_pref.equals(getString(R.string.top_rated_movies_pref))) {
            mPresenter.getTopRatedMovies();

        }
        if (movie_pref.equals(getString(R.string.popular_movies_pref))) {
            mPresenter.getPopularMovies();

        }
        if (movie_pref.equals(getString(R.string.favourite_movies_pref))) {
            mLoadingMoviesIndicator.setVisibility(View.INVISIBLE);
            Loader<Cursor> movieLoader = getSupportLoaderManager().getLoader(MOVIE_LOADER_ID);
            if (movieLoader == null) {
                getSupportLoaderManager().initLoader(MOVIE_LOADER_ID, null, this);
            } else {
                getSupportLoaderManager().restartLoader(MOVIE_LOADER_ID, null, this);
            }

        }
    }

    private void showErrorMessage(String error) {
        mLoadingMoviesIndicator.setVisibility(View.INVISIBLE);
        mMoviesRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessage.setText(error);
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(Movie movie) {
        MovieDetailActivity.start(this, movie);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.movie_menu, menu);
        return true;
    }

    private void saveMoviesUserPref(String pref) {
        SharedPreferences.Editor editor = mMoviePref.edit();
        editor.putString(getString(R.string.movie_pref_key), pref);
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.popular_menu:
                saveMoviesUserPref(getString(R.string.popular_movies_pref));
                loadMoviesData(getString(R.string.popular_movies_pref));
                break;
            case R.id.top_rated_menu:
                saveMoviesUserPref(getString(R.string.top_rated_movies_pref));
                loadMoviesData(getString(R.string.top_rated_movies_pref));
                break;
            case R.id.favourite_menu:
                saveMoviesUserPref(getString(R.string.favourite_movies_pref));
                loadMoviesData(getString(R.string.favourite_movies_pref));
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void showMovieData(List<Movie> movies) {
        mLoadingMoviesIndicator.setVisibility(View.INVISIBLE);
        mMovieAdapter.setMovies(movies);
        mErrorMessage.setVisibility(View.INVISIBLE);
        mMoviesRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showErrorOccurredMessage() {
        showErrorMessage(getString(R.string.error_occurred_error_message));
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(MovieMainActivity.this,
                MoviesContract.MovieEntry.CONTENT_URI,
                null,
                null,
                null,
                MoviesContract.MovieEntry.COLUMN_RATING + " DESC");
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        List<Movie> movieList = FavouriteMoviesUtil.cursorToMovies(cursor);
        if (movieList == null) {
            showErrorOccurredMessage();
            return;
        }
        if (movieList.isEmpty()) {
            showErrorMessage(getString(R.string.no_favourite_movies_user_message));
        } else {
            showMovieData(movieList);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }
}
