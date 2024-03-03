package com.mrboomdev.awery.util;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mrboomdev.awery.AweryApp;
import com.mrboomdev.awery.catalog.template.CatalogList;
import com.mrboomdev.awery.catalog.template.CatalogMedia;
import com.mrboomdev.awery.data.db.DBCatalogList;
import com.mrboomdev.awery.data.db.DBCatalogMedia;
import com.mrboomdev.awery.ui.activity.MediaActivity;
import com.mrboomdev.awery.util.ui.DialogBuilder;
import com.mrboomdev.awery.util.ui.ViewUtil;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import ani.awery.databinding.PopupMediaActionsBinding;
import ani.awery.databinding.PopupMediaBookmarkBinding;

public class MediaUtils {
	public static final String ACTION_INFO = "info";
	public static final String ACTION_WATCH = "watch";
	public static final String ACTION_COMMENTS = "comments";
	public static final String ACTION_RELATIONS = "relations";

	public static void launchMediaActivity(Context context, @NonNull CatalogMedia media, String action) {
		var intent = new Intent(context, MediaActivity.class);
		intent.putExtra("media", media.toString());
		intent.putExtra("action", action);
		context.startActivity(intent);
	}

	public static void launchMediaActivity(Context context, CatalogMedia media) {
		launchMediaActivity(context, media, ACTION_INFO);
	}

	public static void openMediaActionsMenu(Context context, @NonNull CatalogMedia media) {
		final var dialog = new AtomicReference<Dialog>();
		var inflater = LayoutInflater.from(context);
		var binding = PopupMediaActionsBinding.inflate(inflater);

		binding.title.setText(media.title);

		binding.play.setOnClickListener(v -> {
			launchMediaActivity(context, media, ACTION_WATCH);
			dialog.get().dismiss();
		});

		binding.share.setOnClickListener(v -> {
			shareMedia(context, media);
			dialog.get().dismiss();
		});

		binding.bookmark.setOnClickListener(v -> {
			openMediaBookmarkMenu(context, media);
			dialog.get().dismiss();
		});

		var sheet = new BottomSheetDialog(context);
		dialog.set(sheet);

		sheet.getBehavior().setPeekHeight(1000);
		sheet.setContentView(binding.getRoot());
		sheet.show();

		limitDialogSize(sheet.getWindow());
	}

	private static void limitDialogSize(Window window) {
		if(AweryApp.getConfiguration().screenWidthDp > 400) {
			window.setLayout(ViewUtil.dpPx(400), ViewGroup.LayoutParams.MATCH_PARENT);
		}
	}

	public static void shareMedia(Context context, @NonNull CatalogMedia media) {
		new ShareCompat.IntentBuilder(context)
				.setType("text/plain")
				.setText("https://anilist.co/anime/" + media.id)
				.startChooser();
	}

	public static void requestCreateNewList(Context context, OnListCreatedListener callback) {
		new DialogBuilder(context)
				.setTitle("Create new list")
				.addInputField(1, "List name")
				.setPositiveButton("Create", dialog -> {
					var input = dialog.getField(1, DialogBuilder.InputField.class);
					var text = input.getText();

					if(text.isBlank()) {
						input.setError("List name cannot be empty");
						return;
					}

					new Thread(() -> {
						try {
							var listsDao = AweryApp.getDatabase().getListDao();
							var list = new CatalogList(text);
							listsDao.insert(DBCatalogList.fromCatalogList(list));

							AweryApp.runOnUiThread(() -> callback.onListCreated(list));
						} catch(Exception e) {
							AweryApp.toast("Failed to create list");
							e.printStackTrace();
						}
					}).start();

					dialog.dismiss();
				})
				.setCancelButton("Cancel", DialogBuilder::dismiss)
				.show();


		/*var dialog = new AlertDialog.Builder(context);
		var hint = "List name";

		var inputLayout = new TextInputLayout(dialog.getContext());
		inputLayout.setHint(hint);
		var input = new TextInputEditText(dialog.getContext());
		inputLayout.addView(input);

		dialog.setTitle("Create new list")
				.setView(inputLayout)
				.setPositiveButton("Create", (dialog1, which) -> {
					var text = input.getText();

					if(text == null || text.toString().isBlank()) {
						inputLayout.setError("List name cannot be empty");
						return;
					}

					new Thread(() -> {
						try {
							var listsDao = AweryApp.getDatabase().getListDao();
							var list = new CatalogList(text.toString());
							listsDao.insert(DBCatalogList.fromCatalogList(list));

							AweryApp.runOnUiThread(() -> callback.onListCreated(list));
						} catch(Exception e) {
							AweryApp.toast("Failed to create list");
							e.printStackTrace();
						}
					}).start();

					dialog1.dismiss();
				})
				.setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
				.show();*/
	}

	public static void requestDeleteList(Context context, CatalogList list) {
		new MaterialAlertDialogBuilder(context)
				.setTitle("Delete list")
				.setMessage("Are you sure you want to delete this list?")
				.setPositiveButton("Delete", (dialog1, which) -> {
					dialog1.dismiss();

					// TODO
				})
				.setNegativeButton("Cancel", (dialog1, which) -> {
					dialog1.dismiss();
				}).show();
	}

	public static void requestEditList(Context context, CatalogList list) {
		new MaterialAlertDialogBuilder(context)
				.setTitle("Edit list")
				.setPositiveButton("Edit", (dialog1, which) -> {
					dialog1.dismiss();

					// TODO
				})
				.setNegativeButton("Cancel", (dialog1, which) -> {
					dialog1.dismiss();
				}).show();
	}

	private interface OnListCreatedListener {
		void onListCreated(CatalogList list);
	}

	public static void openMediaBookmarkMenu(Context context, CatalogMedia media) {
		new Thread(() -> {
			var lists = AweryApp.getDatabase().getListDao().getAll();
			var current = AweryApp.getDatabase().getMediaDao().get(media.globalId);
			var mediaDao = AweryApp.getDatabase().getMediaDao();

			AweryApp.runOnUiThread(() -> {
				final var dialog = new AtomicReference<Dialog>();
				var binding = PopupMediaBookmarkBinding.inflate(LayoutInflater.from(context));
				var checked = new HashMap<String, Boolean>();

				//TODO: Show this button after the list creation dialog will be finished
				binding.create.setVisibility(View.GONE);

				for(var list : lists) {
					var item = list.toCatalogList();

					var checkbox = new MaterialCheckBox(context);
					checkbox.setText(item.getTitle());
					binding.lists.addView(checkbox);

					if(current != null && current.lists.contains(item.getId())) {
						checkbox.setChecked(true);
					}

					checkbox.setOnCheckedChangeListener((buttonView, isChecked) ->
							checked.put(item.getId(), isChecked));
				}

				binding.create.setOnClickListener(v -> requestCreateNewList(context, list -> {
					var checkbox = new MaterialCheckBox(context);
					checkbox.setText(list.getTitle());
					binding.lists.addView(checkbox);

					checkbox.setOnCheckedChangeListener((buttonView, isChecked) ->
							checked.put(list.getId(), isChecked));
				}));

				binding.save.setOnClickListener(v -> {
					if(checked.isEmpty()) {
						dialog.get().dismiss();
						return;
					}

					new Thread(() -> {
						try {
							media.clearBookmarks();

							for(var entry : checked.entrySet()) {
								if(!entry.getValue()) continue;
								media.addToList(entry.getKey());
							}

							var dbItem = DBCatalogMedia.fromCatalogMedia(media);
							mediaDao.insert(dbItem);

							AweryApp.toast("Saved successfully!");
						} catch(Exception e) {
							AweryApp.toast("Failed to save!");
							e.printStackTrace();
						}
					}).start();

					dialog.get().dismiss();
				});

				binding.cancel.setOnClickListener(v -> dialog.get().dismiss());

				var sheet = new BottomSheetDialog(context);
				dialog.set(sheet);

				sheet.getBehavior().setPeekHeight(1000);
				sheet.setContentView(binding.getRoot());
				sheet.show();

				limitDialogSize(sheet.getWindow());
			});
		}).start();
	}
}