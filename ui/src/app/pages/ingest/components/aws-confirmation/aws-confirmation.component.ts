import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SnackbarService } from 'src/app/core/services/snackbar.service';


@Component({
  selector: 'app-aws-confirmation',
  templateUrl: './aws-confirmation.component.html',
  styleUrls: ['./aws-confirmation.component.css']
})
export class AwsConfirmationComponent {
  @Input() projectId: number;
  @Input() userId: number;
  @Input() folderId: number;

  constructor(
    public modal: NgbActiveModal,
    private snakbar: SnackbarService,
  ) { }

  ngOnInit(): void {
  }

  public deleteFolder(): void {
    this.modal.close();
  }
}
