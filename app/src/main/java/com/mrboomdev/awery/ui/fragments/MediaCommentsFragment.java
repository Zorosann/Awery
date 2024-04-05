package com.mrboomdev.awery.ui.fragments;

import static com.mrboomdev.awery.app.AweryApp.runDelayed;
import static com.mrboomdev.awery.app.AweryApp.runOnUiThread;
import static com.mrboomdev.awery.app.AweryApp.stream;
import static com.mrboomdev.awery.app.AweryApp.toast;
import static com.mrboomdev.awery.util.ui.ViewUtil.MATCH_PARENT;
import static com.mrboomdev.awery.util.ui.ViewUtil.createLinearParams;
import static com.mrboomdev.awery.util.ui.ViewUtil.dpPx;
import static com.mrboomdev.awery.util.ui.ViewUtil.setBottomPadding;
import static com.mrboomdev.awery.util.ui.ViewUtil.setRightMargin;
import static com.mrboomdev.awery.util.ui.ViewUtil.setRightPadding;
import static com.mrboomdev.awery.util.ui.ViewUtil.setTopPadding;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.mrboomdev.awery.R;
import com.mrboomdev.awery.app.AweryApp;
import com.mrboomdev.awery.databinding.LayoutLoadingBinding;
import com.mrboomdev.awery.databinding.WidgetCommentBinding;
import com.mrboomdev.awery.databinding.WidgetCommentSendBinding;
import com.mrboomdev.awery.extensions.Extension;
import com.mrboomdev.awery.extensions.ExtensionProvider;
import com.mrboomdev.awery.extensions.ExtensionsFactory;
import com.mrboomdev.awery.extensions.data.CatalogComment;
import com.mrboomdev.awery.extensions.data.CatalogMedia;
import com.mrboomdev.awery.extensions.request.ReadMediaCommentsRequest;
import com.mrboomdev.awery.util.StringUtil;
import com.mrboomdev.awery.util.UniqueIdGenerator;
import com.mrboomdev.awery.util.exceptions.ExceptionDescriptor;
import com.mrboomdev.awery.util.exceptions.InvalidSyntaxException;
import com.mrboomdev.awery.util.ui.ViewUtil;
import com.mrboomdev.awery.util.ui.adapter.SingleViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class MediaCommentsFragment extends Fragment {
	private final SingleViewAdapter.BindingSingleViewAdapter<LayoutLoadingBinding> loadingAdapter;
	private final WeakHashMap<CatalogComment, Integer> scrollPositions = new WeakHashMap<>();
	private final CommentsAdapter commentsAdapter = new CommentsAdapter();
	private final List<CatalogComment> currentCommentsPath = new ArrayList<>();
	private final Runnable backPressCallback;
	private LinearLayoutManager layoutManager;
	private RecyclerView recycler;
	private Runnable onCloseRequestListener;
	private WidgetCommentSendBinding sendBinding;
	private SwipeRefreshLayout swipeRefresher;
	private ConcatAdapter adapter;
	private List<ExtensionProvider> providers;
	private CatalogMedia media;
	private CatalogComment comment;

	public MediaCommentsFragment() {
		this(null);
	}

	public MediaCommentsFragment(CatalogMedia media) {
		loadingAdapter = SingleViewAdapter.fromBindingDynamic(parent -> {
			var inflater = LayoutInflater.from(parent.getContext());
			return LayoutLoadingBinding.inflate(inflater, parent, false);
		});

		backPressCallback = () -> {
			if(!currentCommentsPath.isEmpty()) {
				currentCommentsPath.remove(currentCommentsPath.size() - 1);
			}

			if(currentCommentsPath.isEmpty()) {
				if(onCloseRequestListener != null) {
					onCloseRequestListener.run();
					return;
				}

				requireActivity().finish();
				return;
			}

			setComment(currentCommentsPath.get(currentCommentsPath.size() - 1));
		};

		setMedia(media);
	}

	public void setOnCloseRequestListener(Runnable listener) {
		this.onCloseRequestListener = listener;
	}

	@Override
	public void onResume() {
		super.onResume();
		AweryApp.addOnBackPressedListener(requireActivity(), backPressCallback);
	}

	@Override
	public void onPause() {
		super.onPause();
		AweryApp.removeOnBackPressedListener(requireActivity(), onCloseRequestListener);
	}

	public void setMedia(CatalogMedia media) {
		if(media == null) return;
		this.media = media;
		if(adapter == null) return;

		loadData(null, false);
	}

	private void setComment(@Nullable CatalogComment comment) {
		this.comment = comment;

		if(comment != null && !currentCommentsPath.contains(comment)) {
			currentCommentsPath.add(comment);
		}

		commentsAdapter.setData(comment);

		//TODO: Load an avatar received from the extension
		sendBinding.avatarWrapper.setVisibility(View.GONE);

		sendBinding.getRoot().setVisibility(
				comment != null && comment.canComment ?
				View.VISIBLE : View.GONE);

		var scrollPosition = scrollPositions.get(comment);

		if(scrollPosition != null) {
			//runDelayed(() -> layoutManager.scrollVerticallyBy(scrollPosition, recycler, State), 1);
		}
	}

	private void loadData(CatalogComment parent, boolean isReload) {
		if(this.comment != null) {
			scrollPositions.put(this.comment, recycler.computeVerticalScrollOffset());
		}

		if(providers != null) {
			if(providers.isEmpty()) {
				loadingAdapter.getBinding(binding -> {
					binding.title.setText(R.string.nothing_found);
					binding.message.setText(R.string.no_comment_extensions_message);

					binding.info.setVisibility(View.VISIBLE);
					binding.progressBar.setVisibility(View.GONE);
				});

				swipeRefresher.setRefreshing(false);
				return;
			}

			if(!isReload) {
				loadingAdapter.getBinding(binding -> {
					binding.info.setVisibility(View.GONE);
					binding.progressBar.setVisibility(View.VISIBLE);
				});

				loadingAdapter.setEnabled(true);
				setComment(null);
			}

			if(providers.size() > 1) {
				toast("Sorry, but you cannot currently use more than 1 comment extension :(");
			}

			var request = new ReadMediaCommentsRequest()
					.setPage(0)
					.setParentComment(parent)
					.setMedia(media);

			providers.get(0).readMediaComments(request, new ExtensionProvider.ResponseCallback<>() {
				@Override
				public void onSuccess(CatalogComment parent) {
					runOnUiThread(() -> {
						if(getContext() == null) return;

						swipeRefresher.setRefreshing(false);
						loadingAdapter.setEnabled(false);

						setComment(parent);
					});
				}

				@Override
				public void onFailure(Throwable e) {
					loadingAdapter.getBinding(binding -> runOnUiThread(() -> {
						if(getContext() == null) return;
						swipeRefresher.setRefreshing(false);

						var descriptor = new ExceptionDescriptor(e);
						binding.title.setText(descriptor.getTitle(getContext()));
						binding.message.setText(descriptor.getMessage(getContext()));

						binding.info.setVisibility(View.VISIBLE);
						binding.progressBar.setVisibility(View.GONE);

						loadingAdapter.setEnabled(true);
						sendBinding.getRoot().setVisibility(View.GONE);
					}));

					Log.e("MediaCommentsFragment", "Failed to load comments!", e);
				}
			});
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		providers = stream(ExtensionsFactory.getExtensions(Extension.FLAG_WORKING))
				.map(extension -> extension.getProviders(ExtensionProvider.FEATURE_MEDIA_COMMENTS))
				.flatMap(AweryApp::stream)
				.sorted().toList();

		setMedia(media);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		layoutManager = new LinearLayoutManager(inflater.getContext());

		swipeRefresher = new SwipeRefreshLayout(inflater.getContext());
		swipeRefresher.setOnRefreshListener(() -> loadData(null, true));

		var parentLayout = new LinearLayoutCompat(inflater.getContext());
		parentLayout.setOrientation(LinearLayoutCompat.VERTICAL);
		swipeRefresher.addView(parentLayout);

		recycler = new RecyclerView(inflater.getContext());
		recycler.setLayoutManager(layoutManager);
		recycler.setClipToPadding(false);

		ViewUtil.setOnApplyUiInsetsListener(recycler, insets -> {
			var padding = dpPx(8);

			setTopPadding(recycler, insets.top + padding);
			setRightPadding(recycler, insets.right);
			setBottomPadding(recycler, padding);
		}, container);

		adapter = new ConcatAdapter(new ConcatAdapter.Config.Builder()
				.setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build(),
				loadingAdapter, commentsAdapter);

		recycler.setAdapter(adapter);
		parentLayout.addView(recycler, createLinearParams(MATCH_PARENT, 0, 1));

		sendBinding = WidgetCommentSendBinding.inflate(inflater, parentLayout, true);
		sendBinding.getRoot().setVisibility(View.GONE);

		ViewUtil.setOnApplyUiInsetsListener(sendBinding.getRoot(), insets ->
				setRightMargin(sendBinding.getRoot(), insets.right), parentLayout);

		return swipeRefresher;
	}

	private class CommentsAdapter extends RecyclerView.Adapter<CommentViewHolder> {
		private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
		private CatalogComment root;

		public CommentsAdapter() {
			setHasStableIds(true);
		}

		@SuppressLint("NotifyDataSetChanged")
		public void setData(@Nullable CatalogComment root) {
			this.root = root;
			idGenerator.clear();

			if(root != null) {
				for(var item : root.items) {
					item.visualId = idGenerator.getLong();
				}
			}

			notifyDataSetChanged();
		}

		@NonNull
		@Override
		public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			var inflater = LayoutInflater.from(parent.getContext());
			var binding = WidgetCommentBinding.inflate(inflater, parent, false);
			var holder = new CommentViewHolder(binding);

			binding.getRoot().setOnClickListener(v -> {
				var comment = holder.getComment();
				loadData(comment, false);
			});

			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
			var item = root.items.get(position);
			holder.bind(item);
		}

		@Override
		public long getItemId(int position) {
			var item = root.items.get(position);
			return item.visualId;
		}

		@Override
		public int getItemCount() {
			if(root == null) return 0;
			return root.items.size();
		}
	}

	private static class CommentViewHolder extends RecyclerView.ViewHolder {
		private final WidgetCommentBinding binding;
		private CatalogComment comment;

		public CommentViewHolder(@NonNull WidgetCommentBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

		public CatalogComment getComment() {
			return comment;
		}

		@SuppressLint("SetTextI18n")
		public void bind(@NonNull CatalogComment comment) {
			this.comment = comment;
			String date = null;

			if(comment.date != null) {
				try {
					var time = StringUtil.parseDate(comment.date).getTime();
					var now = System.currentTimeMillis();

					date = DateUtils.getRelativeTimeSpanString(
							time, now, DateUtils.SECOND_IN_MILLIS).toString();
				} catch(InvalidSyntaxException e) {
					Log.e("MediaCommentsFragment", "Failed to parse comment date!", e);
					date = comment.date;
				}
			}

			if(date == null) binding.name.setText(comment.authorName);
			else binding.name.setText(comment.authorName + " • " + date);

			binding.message.setText(comment.text);
			binding.likesCount.setText(comment.likes == 0 ? "" : String.valueOf(comment.likes));
			binding.dislikesCount.setText(comment.dislikes == 0 ? "" : String.valueOf(comment.dislikes));
			binding.votesCount.setText((comment.votes == null || comment.votes == 0) ? "" : String.valueOf(comment.votes));
			binding.commentsCount.setText(comment.comments == 0 ? "" : String.valueOf(comment.comments));

			if(comment.likes == CatalogComment.DISABLED) {
				binding.likeIcon.setVisibility(View.GONE);
				binding.likesCount.setVisibility(View.GONE);
				binding.likeButton.setVisibility(View.GONE);
			} else if(comment.likes == CatalogComment.HIDDEN) {
				binding.likeIcon.setVisibility(View.VISIBLE);
				binding.likesCount.setVisibility(View.GONE);
				binding.likeButton.setVisibility(View.VISIBLE);
			} else {
				binding.likeIcon.setVisibility(View.VISIBLE);
				binding.likesCount.setVisibility(View.VISIBLE);
				binding.likeButton.setVisibility(View.VISIBLE);
			}

			if(comment.dislikes == CatalogComment.DISABLED) {
				binding.dislikeIcon.setVisibility(View.GONE);
				binding.dislikesCount.setVisibility(View.GONE);
				binding.dislikeButton.setVisibility(View.GONE);
			} else if(comment.dislikes == CatalogComment.HIDDEN) {
				binding.dislikeIcon.setVisibility(View.VISIBLE);
				binding.dislikesCount.setVisibility(View.GONE);
				binding.dislikeButton.setVisibility(View.VISIBLE);
			} else {
				binding.dislikeIcon.setVisibility(View.VISIBLE);
				binding.dislikesCount.setVisibility(View.VISIBLE);
				binding.dislikeButton.setVisibility(View.VISIBLE);
			}

			if(comment.comments == CatalogComment.DISABLED) {
				binding.commentIcon.setVisibility(View.GONE);
				binding.commentsCount.setVisibility(View.GONE);
				binding.commentButton.setVisibility(View.GONE);
			} else if(comment.comments == CatalogComment.HIDDEN) {
				binding.commentIcon.setVisibility(View.VISIBLE);
				binding.commentsCount.setVisibility(View.GONE);
				binding.commentButton.setVisibility(View.VISIBLE);
			} else {
				binding.commentIcon.setVisibility(View.VISIBLE);
				binding.commentsCount.setVisibility(View.VISIBLE);
				binding.commentButton.setVisibility(View.VISIBLE);
			}

			if(comment.votes != null) binding.votesCount.setVisibility(View.VISIBLE);
			else binding.votesCount.setVisibility(View.GONE);

			Glide.with(binding.icon)
					.clear(binding.icon);

			Glide.with(binding.icon)
					.load(comment.authorAvatar)
					.transition(DrawableTransitionOptions.withCrossFade())
					.into(binding.icon);
		}
	}
}