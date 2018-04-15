package ch.sheremet.katarina.movieapp.moviedetail;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.sheremet.katarina.movieapp.R;
import ch.sheremet.katarina.movieapp.base.BaseActivity;
import ch.sheremet.katarina.movieapp.di.DaggerMovieDetailComponent;
import ch.sheremet.katarina.movieapp.di.MovieDetailComponent;
import ch.sheremet.katarina.movieapp.di.MovieDetailModule;
import ch.sheremet.katarina.movieapp.favouritemovies.data.MovieDbHelper;
import ch.sheremet.katarina.movieapp.favouritemovies.data.MoviesContract;
import ch.sheremet.katarina.movieapp.model.Movie;
import ch.sheremet.katarina.movieapp.model.Review;
import ch.sheremet.katarina.movieapp.model.Trailer;
import ch.sheremet.katarina.movieapp.reviewdetail.ReviewDetailFragment;
import ch.sheremet.katarina.movieapp.utilities.UriUtil;

public class MovieDetailActivity extends BaseActivity
        implements TrailerAdapter.TrailerAdapterOnClickHandler,
        ReviewAdapter.ReviewAdapterOnClickHandler,
        IMovieDetailView {

    private static final String TAG = MovieDetailActivity.class.getSimpleName();
    private static final String MOVIE_PARAM = "movie";
    private static final String REVIEW_FRAGMENT_TAG = "review_detail";

    @BindView(R.id.detail_movie_poster_iv)
    ImageView mPosterIV;
    @BindView(R.id.detail_movie_title_tv)
    TextView mTitleTV;
    @BindView(R.id.detail_plot_summary_tv)
    TextView mPlotSummaryTV;
    @BindView(R.id.detail_rating_tv)
    TextView mRatingTV;
    @BindView(R.id.detail_release_date_tv)
    TextView mReleaseDateTV;
    @BindView(R.id.trailers_rv)
    RecyclerView mTrailersRecyclerView;
    @BindView(R.id.reviews_rv)
    RecyclerView mReviewsRecyclerView;
    @BindView(R.id.add_to_favourite_iv)
    ImageView mFavouriteMovieImageView;

    private TrailerAdapter mTrailersAdapter;
    private ReviewAdapter mReviewsAdapter;
    private Movie mMovie;
    @Inject
    IMovieDetailPresenter mPresenter;

    private boolean mIsMovieFavourite;
    private SQLiteDatabase mDb;

    public static void start(final Context context, final Movie movie) {
        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra(MOVIE_PARAM, movie);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        if ((getIntent() == null) || (!getIntent().hasExtra(MOVIE_PARAM))) {
            finish();
        }
        ButterKnife.bind(this);
        mMovie = getIntent().getParcelableExtra(MOVIE_PARAM);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(mMovie.getOriginalTitle());
        // TODO: show default picture and on error.
        Picasso.get()
                .load(UriUtil.buildPosterUrl(mMovie.getPoster()))
                .into(mPosterIV);
        mPosterIV.setContentDescription(mMovie.getPlotSynopsis());
        mTitleTV.setText(mMovie.getOriginalTitle());
        mPlotSummaryTV.setText(mMovie.getPlotSynopsis());
        mRatingTV.setText(getString(R.string.rating_detail_tv,
                String.valueOf(mMovie.getUserRating())));
        mReleaseDateTV.setText(getString(R.string.release_detail_tv, mMovie.getReleaseDate()));
        MovieDetailComponent component = DaggerMovieDetailComponent
                .builder()
                .movieDetailModule(new MovieDetailModule(this))
                .build();
        component.injectMovieDetailActivity(this);
        setTrailersView();
        setReviewsView();
        mIsMovieFavourite = false;

        // TODO: Refactor
        // TODO: dagger
        MovieDbHelper dbHelper = new MovieDbHelper(this);
        mDb = dbHelper.getWritableDatabase();
        isMovieFavourite();
    }

    private void setTrailersView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        mTrailersRecyclerView.setLayoutManager(linearLayoutManager);
        mTrailersAdapter = new TrailerAdapter(this);
        mTrailersRecyclerView.setAdapter(mTrailersAdapter);
        mPresenter.getTrailers(mMovie.getId());
    }

    private void setReviewsView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        mReviewsRecyclerView.setLayoutManager(linearLayoutManager);
        mReviewsAdapter = new ReviewAdapter(this);
        mReviewsRecyclerView.setAdapter(mReviewsAdapter);
        mPresenter.getReviews(mMovie.getId());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // TODO: implement nav up
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTrailerClick(Trailer trailer) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse(UriUtil.buildYoutubeVideoUrl(trailer.getKey()))));
    }

    @Override
    public void onReviewClick(Review review) {
        ReviewDetailFragment.newInstance(review)
                .show(getSupportFragmentManager(), REVIEW_FRAGMENT_TAG);
    }

    @Override
    public void showReviews(final List<Review> reviews) {
        mReviewsAdapter.setReviews(reviews);
    }

    @Override
    public void showTrailers(final List<Trailer> trailers) {
        mTrailersAdapter.setTrailers(trailers);
    }

    // TODO: refactor
    @OnClick(R.id.add_to_favourite_iv)
    protected void onFavouriteMovieClick(ImageView addToFavourite) {
        if (mIsMovieFavourite) {
            addToFavourite.setImageResource(android.R.drawable.btn_star_big_off);
            mIsMovieFavourite = false;
            System.out.println("Remove Movie output: " + removeMovie());
        } else {
            addToFavourite.setImageResource(android.R.drawable.btn_star_big_on);
            mIsMovieFavourite = true;
            System.out.println("Add Movie output: " + addMovie());
        }
    }


    // TODO: Refactor, dagger, presenter
    private long addMovie() {
        ContentValues cv = new ContentValues();
        cv.put(MoviesContract.MovieEntry.COLUMN_MOVIE_ID, mMovie.getId());
        cv.put(MoviesContract.MovieEntry.COLUMN_TITLE, mMovie.getOriginalTitle());
        cv.put(MoviesContract.MovieEntry.COLUMN_PLOT_SYNOPSIS, mMovie.getPlotSynopsis());
        cv.put(MoviesContract.MovieEntry.COLUMN_RAITING, mMovie.getUserRating());
        cv.put(MoviesContract.MovieEntry.COLUMN_RELEASE_DATE, mMovie.getReleaseDate());
        cv.put(MoviesContract.MovieEntry.COLUMN_POSTER_PATH, mMovie.getPoster());
        return mDb.insert(MoviesContract.MovieEntry.TABLE_NAME, null, cv);
    }

    // TODO: refactor
    private int removeMovie() {
        return mDb.delete(MoviesContract.MovieEntry.TABLE_NAME,
                MoviesContract.MovieEntry.COLUMN_MOVIE_ID + "=" + mMovie.getId(),
                null);
    }

    private void isMovieFavourite() {
        Cursor cursor = mDb.query(MoviesContract.MovieEntry.TABLE_NAME,
                null,
                MoviesContract.MovieEntry.COLUMN_MOVIE_ID + "=" + mMovie.getId(),
                null,
                null,
                null,
                null);
        if (cursor.getCount() == 0) {
            mIsMovieFavourite = false;
            mFavouriteMovieImageView.setImageResource(android.R.drawable.btn_star_big_off);
        } else {
            mIsMovieFavourite = true;
            mFavouriteMovieImageView.setImageResource(android.R.drawable.btn_star_big_on);
        }
        cursor.close();
    }
}
